package com.expleague.ml.meta;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.IntSeq;
import com.expleague.commons.seq.VecSeq;
import com.expleague.commons.math.vectors.impl.vectors.SparseVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.Seq;

/**
 * User: solar
 * Date: 16.07.14
 * Time: 18:01
 */
public interface FeatureMeta {
  String id();

  String description();

  ValueType type();

  enum ValueType {
    VEC(Vec.class),
    SPARSE_VEC(SparseVec.class),
    INTS(IntSeq.class),
    VEC_SEQ(VecSeq.class),
    CHAR_SEQ(CharSeq.class);

    private final Class<? extends Seq<?>> type;

    private ValueType(final Class<? extends Seq<?>> type) {
      this.type = type;
    }

    public Class<? extends Seq<?>> clazz() {
      return type;
    }
  }

  abstract class Stub implements FeatureMeta{
    @Override
    public final boolean equals(final Object o) {
      if (this == o)
        return true;
      if (!(o instanceof FeatureMeta))
        return false;

      final FeatureMeta other = (FeatureMeta) o;
      return id().equals(other.id());
    }

    @Override
    public final int hashCode() {
      return id().hashCode();
    }

    @Override
    public String toString() {
      return id() + ": " + description();
    }
  }
}
