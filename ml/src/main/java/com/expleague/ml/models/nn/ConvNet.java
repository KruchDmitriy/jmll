package com.expleague.ml.models.nn;

import com.expleague.commons.math.TransC1;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.ml.models.nn.layers.InputLayerBuilder;
import com.expleague.ml.models.nn.layers.Layer;
import com.expleague.ml.models.nn.layers.LayerBuilder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ConvNet extends TransC1.Stub implements NeuralNetwork<Vec, Vec> {
  private final NetworkBuilder<Vec>.Network network;
  private final NeuralSpider<Vec> neuralSpider = new NeuralSpider<>();
  private Vec weights;

  public ConvNet(NetworkBuilder<Vec>.Network network) {
    this.network = network;
  }

  @Override
  public Vec apply(Vec input) {
    return neuralSpider.compute(network, input, weights);
  }

  @Override
  public Vec gradient(Vec x) {
    throw new NotImplementedException();
  }

  public Vec weights() {
    return weights;
  }

  @Override
  public int xdim() {
    return -1;
  }

  @Override
  public int ydim() {
    return -1;
  }

  @Override
  public Vec gradientRowTo(Vec x, Vec to, int index) {
    return null;
  }

  public static class InputBuilder implements InputLayerBuilder<Vec> {
    private Vec input;
    private int yStart;
    private ConvInput layer;

    @Override
    public void setInput(Vec input) {
      this.input = input;
    }

    @Override
    public int ydim(Vec input) {
      return input.dim();
    }

    @Override
    public LayerBuilder setPrevBuilder(LayerBuilder layer) {
      return this;
    }

    @Override
    public Layer getLayer() {
      return layer;
    }

    @Override
    public LayerBuilder yStart(int yStart) {
      this.yStart = yStart;
      return this;
    }

    @Override
    public LayerBuilder wStart(int wStart) {
      return this;
    }

    @Override
    public InputLayer build() {
      /* TODO: instead of checking on null, check for size of dimensions */
      if (layer == null) {
        layer = new ConvInput();
      }

      return new ConvInput();
    }

    public class ConvInput implements InputLayer {
      private ConvInput() { }

      @Override
      public void toState(Vec state) {
        VecTools.assign(state.sub(0, input.dim()), input);
      }

      @Override
      public int ydim() {
        return input.dim();
      }

      @Override
      public int yStart() {
        return yStart;
      }

      @Override
      public void initWeights(Vec weights) { }
    }
  }
}
