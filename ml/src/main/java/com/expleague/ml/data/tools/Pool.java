package com.expleague.ml.data.tools;

import com.expleague.commons.math.vectors.Mx;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.mx.ColsVecArrayMx;
import com.expleague.commons.math.vectors.impl.mx.ColsVecSeqMx;
import com.expleague.commons.seq.ArraySeq;
import com.expleague.commons.seq.IntSeq;
import com.expleague.commons.system.RuntimeUtils;
import com.expleague.ml.data.set.VecDataSet;
import com.expleague.ml.meta.*;
import com.expleague.commons.math.vectors.impl.mx.VecBasedMx;
import com.expleague.commons.seq.Seq;
import com.expleague.commons.seq.VecSeq;
import com.expleague.commons.util.ArrayTools;
import com.expleague.commons.util.Pair;
import com.expleague.ml.TargetFunc;
import com.expleague.ml.Vectorization;
import com.expleague.ml.data.set.DataSet;
import com.expleague.ml.data.set.impl.VecDataSetImpl;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.BaseStream;

/**
 * User: solar
 * Date: 07.07.14
 * Time: 20:55
 *
 */
public class Pool<I extends DSItem> {
  protected final DataSetMeta meta;
  protected final List<Pair<? extends TargetMeta, ? extends Seq<?>>> targets;
  protected final Seq<I> items;
  protected final Pair<? extends PoolFeatureMeta, ? extends Seq<?>>[] features;

  protected DataSet<I> data;
  protected VecDataSet vecDataSet;

  public Pool(final DataSetMeta meta,
              final Seq<I> items,
              final Pair<? extends PoolFeatureMeta, ? extends Seq<?>>[] features,
              final Pair<? extends TargetMeta, ? extends Seq<?>>[] targets) {
    this.meta = meta;
    this.targets = new LinkedList<>(Arrays.asList(targets));
    this.items = items;
    this.features = features;
  }

  public DataSetMeta meta() {
    return meta;
  }


  public synchronized DataSet<I> data() {
    if (data == null) {
      final TObjectIntHashMap<I> indices = new TObjectIntHashMap<>((int) (items.length() * 2), 0.7f);
      for (int i = 0; i < items.length(); i++) {
        indices.put(items.at(i), i);
      }
      data = new DataSet.Stub<I>(null) {
        @Override
        public I at(final int i) {
          return items.at(i);
        }

        @Override
        public int length() {
          return items.length();
        }

        @Override
        public DataSetMeta meta() {
          return meta;
        }

        @Override
        public int index(final I obj) {
          return indices.get(obj);
        }

        @Override
        public Class<? extends I> elementType() {
          return items.elementType();
        }
      };
    }
    return data;
  }

  public <T extends DSItem> VecDataSet joinFeatures(final int[] indices, final DataSet<T> ds) {
    final List<Seq<?>> cols = new ArrayList<>();
    boolean hasVecFeatures = false;
    for (int i = 0; i < indices.length; i++) {
      final Seq<?> val = features[indices[i]].second;
      cols.add(val);
      if (!hasVecFeatures && (val instanceof VecSeq || val instanceof ArraySeq)) {
        hasVecFeatures = true;
      }
    }

    final Mx data;
    final int[] cumulativeFeatureLengths;
    if (hasVecFeatures) {
      final List<Seq<?>> seqs = new ArrayList<>(cols.size());
      cumulativeFeatureLengths = new int[cols.size()];

      for (int i = 0; i < cols.size(); i++) {
        final Seq<?> col = cols.get(i);
        final int prevFeaturesLength = i > 0 ? cumulativeFeatureLengths[i - 1] : 0;

        if (col instanceof Vec) {
          seqs.add(new VecSeq(new Vec[]{(Vec) col}));
          cumulativeFeatureLengths[i] = prevFeaturesLength + 1;

        } else if (col instanceof VecSeq) {
          seqs.add(col);
          cumulativeFeatureLengths[i] = prevFeaturesLength + col.length();

        } else if (col instanceof ArraySeq) {
          seqs.add(new VecSeq((ArraySeq) col));
          cumulativeFeatureLengths[i] = prevFeaturesLength + col.length();

        } else {
          throw new IllegalArgumentException("unexpected feature type " + col.getClass().getSimpleName());
        }
      }

      data = new ColsVecSeqMx(seqs.toArray(new VecSeq[seqs.size()]));
    } else {
      data = new ColsVecArrayMx(cols.toArray(new Vec[cols.size()]));
      cumulativeFeatureLengths = ArrayTools.sequence(0, cols.size());
    }

    return new VecDataSetImpl(ds, data, new Vectorization<T>() {
      @Override
      public Vec value(final T subject) {
        return data.row(ds.index(subject));
      }

      @Override
      public FeatureMeta meta(final int findex) {
        final int search = Arrays.binarySearch(cumulativeFeatureLengths, findex);
        final int sourceFeatureIdx = search >= 0 ? search : -search - 1;
        return features[indices[sourceFeatureIdx]].first;
      }

      @Override
      public int dim() {
        return indices.length;
      }
    });
  }

  public VecDataSet vecData() {
    if (vecDataSet == null) {
      final Class[] supportedFeatureTypes = new Class[]{Vec.class, VecSeq.class};
      final DataSet<I> ds = data();
      final TIntArrayList toJoin = new TIntArrayList(features.length);
      for (int i = 0; i < features.length; i++) {
        final Pair<? extends PoolFeatureMeta, ? extends Seq<?>> feature = features[i];
        for (final Class clazz : supportedFeatureTypes) {
          if (clazz.isAssignableFrom(feature.getFirst().type().clazz())) {
            toJoin.add(i);
            break;
          }
        }
      }
      vecDataSet = joinFeatures(toJoin.toArray(), ds);
    }
    return vecDataSet;
  }

  public void addTarget(final TargetMeta meta, final Seq<?> target) {
    targets.add(Pair.create(meta, target));
  }

  public <T extends TargetFunc> T target(final Class<T> targetClass) {
    for (int i = targets.size() - 1; i >= 0; i--) {
      final T target = RuntimeUtils.newInstanceByAssignable(targetClass, targets.get(i).second, targets.get(i).getFirst().associated());
      if (target != null)
        return target;
    }
    try {
      return multiTarget(targetClass);
    } catch (Exception e) {
      throw new RuntimeException("No proper constructor found", e);
    }
  }

  public Seq<?> target(String name) {
    for (int i = targets.size() - 1; i >= 0; i--) {
      if (targets.get(i).first.id().equals(name))
        return targets.get(i).second;
    }
    throw new RuntimeException("No such target: " + name);
  }

  public Seq<?> target(int index) {
    return targets.get(index).second;
  }

  public <T extends TargetFunc> T multiTarget(final Class<T> targetClass) {
    final Mx targetsValues = new VecBasedMx(size(), targets.size());
    for (int j = 0; j < targets.size(); j++) {
      final Seq<?> target = targets.get(j).second;
      if (target instanceof Vec) {
        VecTools.assign(targetsValues.col(j), (Vec) target);
      }
      else if (target instanceof IntSeq) {
        final IntSeq intSeq = (IntSeq) target;
        for (int i = 0; i < target.length(); i++)
          targetsValues.set(i, j, intSeq.intAt(i));
      }
      else {
        throw new RuntimeException("Unsupported target type: " + target.getClass().getName());
      }
    }

    final T target = RuntimeUtils.newInstanceByAssignable(targetClass, targetsValues, data());
    if (target != null) {
      return target;
    } else {
      throw new RuntimeException("No proper constructor found");
    }
  }

  public int size() {
    return items.length();
  }

  public DataSet<?> data(final String dsId) {
    final DataSet<I> data = data();
    if (data.meta().id().equals(dsId))
      return data;
    throw new IllegalArgumentException("No such dataset in the pool");
  }


  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != getClass())
      return false;

    final Pool other = (Pool) obj;
    return new EqualsBuilder().append(meta, other.meta).
        append(targets, other.targets).
        append(items, other.items).
        append(features, other.features).
        isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(meta).
        append(targets).
        append(items).
        append(features).
        toHashCode();
  }

  public PoolFeatureMeta[] features() {
    final PoolFeatureMeta[] result = new PoolFeatureMeta[features.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = features[i].first;
    }
    return result;
  }

  public <T extends TargetFunc> T target(String name, Class<T> targetClass) {
    for (int i = targets.size() - 1; i >= 0; i--) {
      final Pair<? extends TargetMeta, ? extends Seq<?>> current = targets.get(i);
      if (!current.first.id().equals(name))
        continue;
      final T target = RuntimeUtils.newInstanceByAssignable(targetClass, current.second, current.getFirst().associated());
      if (target != null)
        return target;
    }

    throw new RuntimeException("No such target: " + name + " of type " + targetClass.getSimpleName());
  }

  public <T> T feature(int findex, int iindex) {
    //noinspection unchecked
    return (T)features[findex].second.at(iindex);
  }

  public TargetFunc targetByName(String metricName) {
    final String loss;
    String name = null;
    if (metricName.contains("(")) { // has taget name
      final int nameIndex = metricName.indexOf("(") + 1;
      loss = metricName.substring(0, nameIndex);
      name = metricName.substring(nameIndex + 1, metricName.length() - 1);
    }
    else loss = metricName;

    final Class<? extends TargetFunc> lossFunc = DataTools.targetByName(loss);

    return name != null ? target(name, lossFunc) : target(lossFunc);
  }
}
