package com.spbsu.ml.models.gpf.weblogmodel;

import java.util.List;


import com.spbsu.commons.math.vectors.impl.mx.VecBasedMx;
import com.spbsu.ml.models.gpf.ClickProbabilityModel;
import com.spbsu.ml.models.gpf.Session;

/**
 * Created by irlab on 03.10.2014.
 */
public class WebLogV1ClickProbabilityModel implements ClickProbabilityModel<BlockV1> {
  private VecBasedMx clickProbability = new VecBasedMx(BlockV1.ResultType.values().length, BlockV1.ResultGrade.values().length);

  @Override
  public void trainClickProbability(List<Session<BlockV1>> dataset) {
    VecBasedMx shows = new VecBasedMx(BlockV1.ResultType.values().length, BlockV1.ResultGrade.values().length);
    VecBasedMx clicks = new VecBasedMx(BlockV1.ResultType.values().length, BlockV1.ResultGrade.values().length);
    for (Session<BlockV1> ses: dataset) {
      BlockV1 block1 = ses.getBlock(Session.R0_ind);
      shows.adjust(block1.resultType.ordinal(), block1.resultGrade.ordinal(), 1);
      if (ses.hasClickOn(Session.R0_ind))
        clicks.adjust(block1.resultType.ordinal(), block1.resultGrade.ordinal(), 1);
    }

    double[] shows_result_type = new double[BlockV1.ResultType.values().length];
    double[] clicks_result_type = new double[BlockV1.ResultType.values().length];
    double shows_all = 0;
    double clicks_all = 0;
    for (int i = 0; i < BlockV1.ResultType.values().length; i++) {
      for (int j = 0; j < BlockV1.ResultGrade.values().length; j++) {
        shows_result_type[i] += shows.get(i, j);
        clicks_result_type[i] += clicks.get(i, j);
      }
      shows_all += shows_result_type[i];
      clicks_all += clicks_result_type[i];
    }

    double ctr_all = clicks_all / shows_all;
    for (int i = 0; i < BlockV1.ResultType.values().length; i++) {
      double prob_click_result_type = (clicks_result_type[i] + 10 * ctr_all) / (shows_result_type[i] + 10);
      for (int j = 0; j < BlockV1.ResultGrade.values().length; j++) {
        double prob = (clicks.get(i, j) + 10 * prob_click_result_type) / (shows.get(i, j) + 10);
        clickProbability.set(i, j, prob);
      }
    }
  }

  public double getClickGivenViewProbability(BlockV1 b) {
    switch (b.blockType) {
      case RESULT:
        return clickProbability.get(b.resultType.ordinal(), b.resultGrade.ordinal());
      case Q:
        return 1. - 1e-6; // always observed
      case S:
        return 0; // never observed
      case E:
        return 1. - 1e-6; // always observed
    }
    throw new IllegalStateException("unknown ResultType: " + b);
  }

}
