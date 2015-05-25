package com.spbsu.ml.models;

import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.ml.Trans;


public class ShifftedTrans extends Trans.Stub {
  private final double mean;
  private final Trans trans;
  public ShifftedTrans(Trans result, double v) {
    this.trans = result;
    this.mean = v;
  }

  @Override
  public int xdim() {
    return trans.xdim();
  }

  @Override
  public int ydim() {
    return trans.ydim();
  }

  @Override
  public Vec trans(Vec x) {
    final Vec res = trans.trans(x);
    return res.adjust(mean);
  }

}
