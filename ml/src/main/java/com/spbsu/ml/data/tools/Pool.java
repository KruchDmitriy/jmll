package com.spbsu.ml.data.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import com.spbsu.commons.math.vectors.Mx;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.impl.mx.ColsVecArrayMx;
import com.spbsu.commons.seq.Seq;
import com.spbsu.commons.system.RuntimeUtils;
import com.spbsu.commons.util.ArrayTools;
import com.spbsu.commons.util.Pair;
import com.spbsu.ml.TargetFunc;
import com.spbsu.ml.Vectorization;
import com.spbsu.ml.data.set.DataSet;
import com.spbsu.ml.data.set.VecDataSet;
import com.spbsu.ml.data.set.impl.VecDataSetImpl;
import com.spbsu.ml.meta.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

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

  public Pool(final DataSetMeta meta,
              final Seq<I> items,
              final Pair<? extends PoolFeatureMeta, ? extends Seq<?>>[] features,
              final Pair<? extends TargetMeta, ? extends Seq<?>>[] targets) {
    this.meta = meta;
    this.targets = Arrays.asList(targets);
    this.items = items;
    this.features = features;
  }

  public DataSetMeta meta() {
    return meta;
  }

  DataSet<I> data;
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
      };
    }
    return data;
  }

  private <T extends DSItem> VecDataSet joinFeatures(final int[] indices, final DataSet<T> ds) {
    final List<Vec> cols = new ArrayList<>();
    for (int i = 0; i < indices.length; i++) {
      cols.add((Vec)features[indices[i]].second);
    }

    final Mx data = new ColsVecArrayMx(cols.toArray(new Vec[cols.size()]));
    return new VecDataSetImpl(ds, data, new Vectorization<T>() {
      @Override
      public Vec value(final T subject) {
        return data.row(ds.index(subject));
      }

      @Override
      public FeatureMeta meta(final int findex) {
        return features[indices[findex]].first;
      }

      @Override
      public int dim() {
        return indices.length;
      }
    });
  }

  public VecDataSet vecData() {
    final DataSet<I> ds = data();
    final TIntArrayList toJoin = new TIntArrayList(features.length);
    for (int i = 0; i < features.length; i++) {
      Pair<? extends PoolFeatureMeta, ? extends Seq<?>> feature = features[i];
      if (feature.getFirst().associated() == ds && Vec.class.isAssignableFrom(feature.getFirst().type().clazz()))
        toJoin.add(i);
    }
    return joinFeatures(ArrayTools.sequence(0, features.length), ds);
  }

  public void addTarget(TargetMeta meta, Seq<?> target) {
    targets.add(Pair.create(meta, target));
  }

  public <T extends TargetFunc> T target(Class<T> targetClass) {
    for (int i = targets.size() - 1; i >= 0; i--) {
      final T target = RuntimeUtils.newInstanceByAssignable(targetClass, targets.get(i).second, targets.get(i).getFirst().associated());
      if (target != null)
        return target;
    }
    throw new RuntimeException("No proper constructor found");
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
}
