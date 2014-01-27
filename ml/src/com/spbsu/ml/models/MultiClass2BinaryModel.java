package com.spbsu.ml.models;

import com.spbsu.commons.math.MathTools;
import com.spbsu.commons.math.vectors.Mx;
import com.spbsu.commons.math.vectors.MxIterator;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.util.logging.Logger;
import com.spbsu.ml.Func;

/**
 * User: solar
 * Date: 10.01.14
 * Time: 10:59
 */
public class MultiClass2BinaryModel extends MultiClassModel {
  public static final Logger LOG = Logger.create(MultiClass2BinaryModel.class);
  private final Mx codingMatrix;
  private final Func[] binaryClassifiers;
  private final int dim;

  public MultiClass2BinaryModel(final Mx codingMatrix, Func[] binaryClassifiers) {
    super(new Func[codingMatrix.rows()]);
    LOG.assertTrue(codingMatrix.columns() == binaryClassifiers.length, "Coding matrix columns count must match binary classifiers.");
    final MxIterator mxIterator = codingMatrix.nonZeroes();
    while (mxIterator.advance()) {
      LOG.assertTrue(Math.abs(mxIterator.value()) < MathTools.EPSILON
             || Math.abs(mxIterator.value() - 1.) < MathTools.EPSILON
             || Math.abs(mxIterator.value() + 1.) < MathTools.EPSILON, "Coding matrix must contain elements from {-1,0,1} set.");
    }
    this.codingMatrix = codingMatrix;
    this.binaryClassifiers = binaryClassifiers;
    dim = binaryClassifiers[0].dim();
    for (int i = 0; i < dirs.length; i++) {
      dirs[i] = new DecodeFunc(i);
    }
  }

  private class DecodeFunc extends Func.Stub {
    private final int classNo;

    public DecodeFunc(int classNo) {
      this.classNo = classNo;
    }

    @Override
    public double value(Vec x) {
      double result = 0;
      for (int l = 0; l < codingMatrix.columns(); l++) {
        final double m = codingMatrix.get(l, classNo);
        if (Math.abs(m) < MathTools.EPSILON)
          continue;
        final double value = binaryClassifiers[l].value(x);
        result += value * m;
      }
      return result;
    }

    @Override
    public int dim() {
      return dim;
    }
  }
}
