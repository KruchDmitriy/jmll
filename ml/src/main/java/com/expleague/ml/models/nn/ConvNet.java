package com.expleague.ml.models.nn;

import com.expleague.commons.math.TransC1;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.ThreadLocalArrayVec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.func.generic.Sum;
import com.expleague.ml.models.nn.layers.InputLayerBuilder;
import com.expleague.ml.models.nn.layers.Layer;
import com.expleague.ml.models.nn.layers.LayerBuilder;

import java.io.*;

public class ConvNet extends TransC1.Stub implements NeuralNetwork<Vec, Vec> {
  private final NetworkBuilder<Vec>.Network network;
  private final NeuralSpider<Vec> neuralSpider = new NeuralSpider<>();
  private final ThreadLocalArrayVec gradCache = new ThreadLocalArrayVec();
  private Vec weights;
  private TransC1 target = new Sum();

  public ConvNet(NetworkBuilder<Vec>.Network network) {
    this.network = network;
    weights = new ArrayVec(network.wdim());
    network.initWeights(weights);
  }

  @Override
  public Vec apply(Vec input) {
    return neuralSpider.compute(network, input, weights);
  }

  public Vec apply(Vec argument, Vec weights) {
    return neuralSpider.compute(network, argument, weights);
  }

  public Vec gradientTo(Vec x, Vec weights, TransC1 target, Vec to) {
    neuralSpider.parametersGradient(network, x, target, weights, to);
    return to;
  }

  @Override
  public Vec gradientTo(Vec x, Vec to) {
    neuralSpider.parametersGradient(network, x, target, weights, to);
    return to;
  }

  @Override
  public Vec gradientRowTo(Vec x, Vec to, int index) {
    return gradientTo(x, to);
  }

  public void save(String path) {
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(path))) {
      for (double v : weights.toArray()) {
        dos.writeDouble(v);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void load(String path) {
    try (DataInputStream dos = new DataInputStream(new FileInputStream(path))) {
      for (int i = 0; i < wdim(); i++) {
        weights.set(i, dos.readDouble());
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  public int wdim() {
    return network.wdim();
  }

  public void setWeights(Vec weights) {
    this.weights = weights;
  }

  public void setTarget(TransC1 target) {
    this.target = target;
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

      return layer;
    }

    public class ConvInput implements InputLayer {
      private ConvInput() { }

      @Override
      public void toState(Vec state) {
        VecTools.assign(state.sub(0, input.dim()), input);
      }

      @Override
      public int xdim() {
        return ydim();
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
