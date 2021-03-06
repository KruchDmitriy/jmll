package com.expleague.ml.methods;

import com.expleague.commons.func.impl.WeakListenerHolderImpl;
import com.expleague.commons.math.MathTools;
import com.expleague.commons.math.vectors.Mx;
import com.expleague.commons.math.vectors.MxIterator;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.math.vectors.impl.mx.VecBasedMx;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.IntSeq;
import com.expleague.commons.util.ThreadTools;
import com.expleague.commons.util.cache.CacheStrategy;
import com.expleague.commons.util.cache.impl.FixedSizeCache;
import com.expleague.ml.data.set.VecDataSet;
import com.expleague.ml.loss.LLLogit;
import com.expleague.ml.models.pgm.ProbabilisticGraphicalModel;
import com.expleague.ml.models.pgm.Route;
import com.expleague.ml.models.pgm.SimplePGM;


import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * User: solar
 * Date: 27.01.14
 * Time: 13:29
 */
public class PGMEM extends WeakListenerHolderImpl<SimplePGM> implements VecOptimization<LLLogit> {
  public abstract static class Policy implements Predicate<Route> {
    private final Vec weights;
    private final Route[] routes;
    private Double len;
    private int index = 0;

    protected Policy(final ProbabilisticGraphicalModel pgm) {
      weights = new ArrayVec(pgm.knownRoutesCount());
      routes = new Route[pgm.knownRoutesCount()];
    }
    protected void addOption(final Route r, final double w) {
      weights.set(index, w);
      routes[index++] = r;
    }

    public Route next(final FastRandom rng) {
      if (len == null)
        len = VecTools.l1(weights);
      return index == 0 ? null : routes[rng.nextSimple(weights, len)];
    }

    public Policy clear() {
      VecTools.scale(weights, 0.);
      len = null;
      index = 0;
      return this;
    }
  }

  public static final Function<ProbabilisticGraphicalModel, Policy> MOST_PROBABLE_PATH = argument -> new Policy(argument) {
    @Override
    public boolean test(final Route route) {
      addOption(route, 1.);
      return true;
    }
  }.clear();

  public static final Function<ProbabilisticGraphicalModel, Policy> LAPLACE_PRIOR_PATH = argument -> new Policy(argument) {
    @Override
    public boolean test(final Route route) {
      addOption(route, route.p() * prior(route.length()));
      return false;
    }
    private double prior(final int length) {
      return Math.exp(-length-1);
    }
  }.clear();

  public static final Function<ProbabilisticGraphicalModel, Policy> GAMMA_PRIOR_PATH = argument -> new Policy(argument) {
    @Override
    public boolean test(final Route route) {
      addOption(route, route.p() * prior(route.length()));
      return false;
    }
    private double prior(final int length) {
      final double meanERouteLength = ((SimplePGM) argument).meanERouteLength;
      return meanERouteLength > 1 ? length * length * Math.exp(-length/ (meanERouteLength/ 3 * 0.7)) : 1;
    }
  }.clear();

  public static final Function<ProbabilisticGraphicalModel, Policy> POISSON_PRIOR_PATH = argument -> {
    final double meanLen = ((SimplePGM)argument).meanERouteLength;
    return new Policy(argument) {
      @Override
      public boolean test(final Route route) {
        addOption(route, route.p() * prior(route.length()));
        return false;
      }
      private double prior(final int length) {
        return meanLen > 1 ? MathTools.poissonProbability((meanLen - 1) * 0.5, length - 1) : Math.exp(-length);
      }
    }.clear();
  };

  public static final Function<ProbabilisticGraphicalModel, Policy> FREQ_DENSITY_PRIOR_PATH = new Function<ProbabilisticGraphicalModel, Policy>() {
    final WeakHashMap<ProbabilisticGraphicalModel, Vec> cache = new WeakHashMap<>();
    @Override
    public Policy apply(final ProbabilisticGraphicalModel argument) {
      Vec freqs = cache.get(argument);
      if (freqs == null) {
        freqs = new ArrayVec(10000);
        for (int i = 0; i < argument.knownRoutesCount(); i++) {
          final Route r = argument.knownRoute(i);
          if (r.length() < freqs.dim())
            freqs.adjust(r.length(), r.p());
        }
        synchronized (cache) {
          cache.put(argument, freqs);
        }
      }
      final double unknownWeight = 1 - VecTools.norm1(freqs);
      final int knownRootsCount = argument.knownRoutesCount();

      final Vec finalFreqs = freqs;
      return new Policy(argument) {
        @Override
        public boolean test(final Route route) {
          final double prior = finalFreqs.get(route.length());
          addOption(route, route.p() * (prior > 0 ? prior : 2 * unknownWeight / knownRootsCount));
          return false;
        }
      }.clear();
    }
  };

  private final Function<ProbabilisticGraphicalModel, Policy> policyFactory;
  private final Mx topology;
  private final int iterations;
  private final double step;
  private final FastRandom rng;

  @SuppressWarnings("unused")
  public PGMEM(final Mx topology, final double smoothing, final int iterations) {
    this(topology, smoothing, iterations, new FastRandom(), MOST_PROBABLE_PATH);
  }

  public PGMEM(final Mx topology, final double smoothing, final int iterations, final FastRandom rng, final Function<ProbabilisticGraphicalModel, Policy> policy) {
    this.policyFactory = policy;
    this.topology = topology;
    this.iterations = iterations;
    this.step = smoothing;
    this.rng = rng;
  }

  @Override
  public SimplePGM fit(final VecDataSet learn, final LLLogit ll) {
    final ThreadPoolExecutor executor = ThreadTools.createBGExecutor(PGMEM.class.getName(), learn.length());

    SimplePGM currentPGM = new SimplePGM(topology);
    final FixedSizeCache<IntSeq, Policy> cache = new FixedSizeCache<>(10000, CacheStrategy.Type.LRU);
    final int[][] cpds = new int[learn.length()][];
    final Mx data = learn.data();
    for (int j = 0; j < data.rows(); j++) {
      cpds[j] = currentPGM.extractControlPoints(data.row(j));
    }

    for (int t = 0; t < iterations; t++) {
      cache.clear();
      final Route[] eroutes = new Route[learn.length()];
      final SimplePGM finalCurrentPGM = currentPGM;
      { // E-step
        final CountDownLatch latch = new CountDownLatch(cpds.length);

        for (int j = 0; j < cpds.length; j++) {
          final int finalJ = j;
          executor.execute(() -> {
            final Policy policy;
            synchronized (cache) {
              policy = cache.get(new IntSeq(cpds[finalJ]), argument -> {
                final Policy policy1 = policyFactory.apply(finalCurrentPGM);
                finalCurrentPGM.visit(policy1, cpds[finalJ]);
                return policy1;
              });
            }
            eroutes[finalJ] = policy.next(rng);
            latch.countDown();
          });
        }
        try {
          latch.await();
        } catch (InterruptedException e) {
          // skip
        }
      }

      final Mx next = new VecBasedMx(topology.columns(), new ArrayVec(topology.dim()));
      { // adjusting parameters of Dir(next[i]) by one only if this way is possible
        final MxIterator it = topology.nonZeroes();
        while (it.advance()) {
          if (it.value() > MathTools.EPSILON)
            next.adjust(it.index(), 1.);
        }
      }
      double meanLen = 0;
      { // M-step
        for (final Route eroute : eroutes) {
          if (eroute == null)
            continue;
          meanLen += eroute.length();
          int prev = eroute.dst(0);
          for (int i = 1; i < eroute.length(); i++) {
            next.adjust(prev, prev = eroute.dst(i), 1.);
          }
        }
        meanLen /= eroutes.length;
        for (int i = 0; i < next.rows(); i++) {
          VecTools.normalizeL1(next.row(i)); // assuming weights of nodes are distributed by Dir(next[i]), then optimal parameters will be proportional to pass count
        }
      }
      { // Update PGM
        VecTools.scale(next, step/(1. - step));
        VecTools.append(next, currentPGM.topology);
        VecTools.scale(next, (1. - step));
        currentPGM = new SimplePGM(next, meanLen);
        System.out.println(meanLen);
        invoke(currentPGM);
      }
    }
    executor.shutdown();
    return currentPGM;
  }
}
