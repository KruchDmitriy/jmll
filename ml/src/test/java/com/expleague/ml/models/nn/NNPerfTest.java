package com.expleague.ml.models.nn;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import org.junit.Test;

import java.util.Arrays;

public class NNPerfTest {
  private static final int NUM_SHOTS = 200;
  private static final FastRandom rng = new FastRandom();
  private static class Stat {
    public final double median;
    public final double quart1;
    public final double quart3;

    public Stat(double median, double quart1, double quart3) {
      this.median = median;
      this.quart1 = quart1;
      this.quart3 = quart3;
    }

    @Override
    public String toString() {
      return "median " + median + "; quartiles [" + quart1 + ", " + quart3 + "] ms";
    }
  }

  private void blackHole(Vec x) {
    x.get(rng.nextInt(x.dim()));
  }

  @Test
  public void perceptronMultiTest() {
    System.out.println("perceptron test, forward pass");
    perceptronPerfTest(1024, 1024);
    perceptronPerfTest(2048, 2048);
    perceptronPerfTest(4096, 4096);
  }

  public void perceptronPerfTest(int n_hid1, int n_hid2) {
    System.out.println("config: [1, " + n_hid1 + ", " + n_hid2 + ", 1]");

    double[] times = new double[NUM_SHOTS];
    for (int i = 0; i < NUM_SHOTS; i++) {
      final long start = System.nanoTime();
      Vec result = perceptronForward(n_hid1, n_hid2);

      final long finish = System.nanoTime();
      times[i] = (finish - start) / 1_000_000.;

      blackHole(result);
    }

    System.out.println(stat(times));
  }

  private static Stat stat(double[] array) {
    Arrays.sort(array);

    int median = (array.length + 1) / 2;

    return new Stat(array[median], array[median / 2], array[median + median / 2]);
  }

  public Vec perceptronForward(int n_hid1, int n_hid2) {
    LayeredNetwork nn = new LayeredNetwork(new FastRandom(), 0., 1, n_hid1, n_hid2, 1);
    final ArrayVec input = new ArrayVec(1.);
    final ArrayVec weights = new ArrayVec(nn.numParameters());
    VecTools.fill(weights, 1.);
    return nn.compute(input, weights);
  }
}