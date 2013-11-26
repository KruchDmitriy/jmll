package com.spbsu.ml.models;

import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.VecTools;
import com.spbsu.ml.func.Linear;

import static com.spbsu.commons.math.vectors.VecTools.append;
import static com.spbsu.commons.math.vectors.VecTools.multiply;

/**
 * User: solar
 * Date: 01.03.11
 * Time: 22:30
 */
public class NormalizedLinearModel extends Linear {
  private final double avg;
  private final VecTools.NormalizationProperties props;

  public NormalizedLinearModel(double avg, Vec weights, final VecTools.NormalizationProperties props) {
    super(weights);
    this.avg = avg;
    this.props = props;
  }

  @Override
  public double value(Vec point) {
    Vec x = multiply(props.xTrans, point);
    append(x, props.xMean);
    return super.value(point) + avg;
  }
}
