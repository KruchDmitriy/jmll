package com.expleague.ml.meta.impl;

import com.expleague.ml.data.set.DataSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.TargetMeta;

/**
 * User: solar
 * Date: 01.06.15
 * Time: 17:32
 */
public class TargetFeatureImpl implements TargetMeta {
  private final DataSet<?> owner;
  private final FeatureMeta delegate;

  public TargetFeatureImpl(DataSet<?> owner, FeatureMeta delegate) {
    this.owner = owner;
    this.delegate = delegate;
  }

  @Override
  public DataSet<?> associated() {
    return owner;
  }

  @Override
  public String id() {
    return delegate.id();
  }

  @Override
  public String description() {
    return delegate.description() + " @" + owner.meta().id();
  }

  @Override
  public ValueType type() {
    return delegate.type();
  }
}
