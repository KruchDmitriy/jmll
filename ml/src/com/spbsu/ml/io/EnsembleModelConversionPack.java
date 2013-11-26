package com.spbsu.ml.io;

import com.spbsu.commons.func.types.ConversionDependant;
import com.spbsu.commons.func.types.ConversionPack;
import com.spbsu.commons.func.types.ConversionRepository;
import com.spbsu.commons.func.types.TypeConverter;
import com.spbsu.commons.text.CharSequenceTools;
import com.spbsu.ml.Func;
import com.spbsu.ml.models.Ensemble;

import java.util.StringTokenizer;

/**
 * User: solar
 * Date: 12.08.13
 * Time: 17:16
 */
public class EnsembleModelConversionPack implements ConversionPack<Ensemble, CharSequence> {
  public static class To implements TypeConverter<Ensemble, CharSequence>, ConversionDependant {
    private ConversionRepository repository;

    @Override
    public CharSequence convert(Ensemble from) {
      StringBuilder builder = new StringBuilder();
      builder.append(from.size());
      builder.append("\n");
      builder.append("\n");
      for (int i = 0; i < from.size(); i++) {
        Func model = from.models()[i];
        builder.append(from.models()[i].getClass().getCanonicalName()).append(" ");
        builder.append(from.weight(i)).append("\n");
        builder.append(repository.convert(model, CharSequence.class));
        builder.append("\n");
      }
      builder.delete(builder.length() - 1, builder.length());
      return builder;
    }

    @Override
    public void setConversionRepository(ConversionRepository repository) {
      this.repository = repository;
    }
  }

  public static class From implements TypeConverter<CharSequence, Ensemble>, ConversionDependant {
    private ConversionRepository repository;

    @Override
    public Ensemble convert(CharSequence from) {
      CharSequence[] elements = CharSequenceTools.split(from, "\n\n");
      Func[] models;
      double[] weights;

      try {
        int count = Integer.parseInt(elements[0].toString());
        models = new Func[count];
        weights = new double[count];
        for (int i = 0; i < count; i++) {
          final CharSequence[] lines = CharSequenceTools.split(elements[i + 1], "\n");
          StringTokenizer tok = new StringTokenizer(lines[0].toString(), " ");
          Class<? extends Func> elementClass = (Class<? extends Func>) Class.forName(tok.nextToken());
          weights[i] = Double.parseDouble(tok.nextToken());
          models[i] = repository.convert(elements[i + 1].subSequence(lines[0].length(), elements[i + 1].length()), elementClass);
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Element class not found!", e);
      }
      return new Ensemble(models, weights);
    }

    @Override
    public void setConversionRepository(ConversionRepository repository) {
      this.repository = repository;
    }
  }

  @Override
  public Class<To> to() {
    return To.class;
  }

  @Override
  public Class<From> from() {
    return From.class;
  }
}
