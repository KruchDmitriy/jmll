package com.expleague.ml.models.nn.layers;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.ArraySeq;
import com.expleague.commons.seq.Seq;

import static com.expleague.ml.models.nn.NeuralSpider.BackwardNode;
import static com.expleague.ml.models.nn.NeuralSpider.ForwardNode;

public class OneOutLayer implements OutputLayerBuilder {
  private LayerBuilder prevBuilder;

  private final OutputLayer layer = new OutputLayer() {
    @Override
    public Vec fromState(Vec state) {
      return state.sub(prevStart(), xdim());
    }

    @Override
    public int xdim() {
      return prevDim();
    }

    @Override
    public Seq<ForwardNode> forwardFlow() {
      return new ArraySeq<>(new ForwardNode[0]);
    }

    @Override
    public Seq<BackwardNode> backwardFlow() {
      return ArraySeq.emptySeq(BackwardNode.class);
    }

    @Override
    public Seq<BackwardNode> gradientFlow() {
      return ArraySeq.emptySeq(BackwardNode.class);
    }

    @Override
    public String toString() {
      return "OneOutLayer";
    }
  };

  private int prevStart() {
    return prevBuilder.getLayer().yStart();
  }

  public int prevDim() {
    return prevBuilder.getLayer().ydim();
  }

  @Override
  public OutputLayer getLayer() {
    return layer;
  }

  @Override
  public LayerBuilder setPrevBuilder(LayerBuilder prevBuilder) {
    this.prevBuilder = prevBuilder;
    return this;
  }

  @Override
  public OutputLayer build() {
    return layer;
  }
}
