package com.expleague.ml.models.nn;

import com.expleague.commons.math.MathTools;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.FuncC1;
import com.expleague.ml.func.generic.Const;
import com.expleague.ml.models.nn.layers.FCLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
* User: solar
* Date: 26.05.15
* Time: 11:46
*/
public class LayeredNetwork extends NeuralSpider<Double, Vec> {
  private final NodeType[] nodeTypes;
  private final Random rng;
  private final double dropout;
  private final int[] config;
  private final int dim;
  private final int numParameters;

  public LayeredNetwork(Random rng, double dropout, final int... config) {
    this.rng = rng;
    this.dropout = dropout;
    this.config = config;

    int wStart = 0;
    int stateStart = config[0];
    final List<NodeType> nodeTypes = new ArrayList<>();
    for (int d = 1; d < config.length; d++) {
      final int prevLayerPower = config[d - 1];
      final int wSize = config[d] * config[d - 1];
      nodeTypes.add(new FCLayer(stateStart, config[d],
          stateStart - prevLayerPower, prevLayerPower, wStart, wSize));
      stateStart += config[d];
      wStart += wSize;
    }
    this.dim = stateStart;
    this.numParameters = wStart;
    this.nodeTypes = nodeTypes.toArray(new NodeType[nodeTypes.size()]);
  }
  @Override
  public int dim() {
    return dim;
  }

  @Override
  public int numParameters() {
    return numParameters;
  }

  @Override
  protected Topology topology(final boolean dropout) {
    return new Topology.Stub() {
      @Override
      public int outputCount() {
        return config[config.length - 1];
      }

      @Override
      public boolean isDroppedOut(int nodeIndex) {
        //noinspection SimplifiableIfStatement
        if (!dropout || nodeIndex > nodeTypes.length)
          return false;
        return LayeredNetwork.this.dropout > MathTools.EPSILON && rng.nextDouble() < LayeredNetwork.this.dropout;
      }

      @Override
      public NodeType at(int i) {
        return nodeTypes[i];
      }

      @Override
      public int dim() {
        return dim;
      }

      @Override
      public int length() {
        return nodeTypes.length;
      }
    };
  }
}
