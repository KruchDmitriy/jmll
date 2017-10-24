package com.expleague.expedia;

import com.expleague.commons.math.vectors.impl.vectors.VecBuilder;
import com.expleague.commons.util.logging.Logger;
import com.expleague.expedia.builder.CTRBuilder;
import com.expleague.ml.data.tools.CsvRow;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.PoolBuilder;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.ml.meta.impl.JsonFeatureMeta;
import com.expleague.sbrealty.Deal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class Pool {
  private static final Logger LOG = Logger.create(Pool.class);

  private static final String[] COLUMNS = new String[]{
          "date_time",
          "user_id",
          "user_location_city",
          "srch_ci",
          "srch_co",
          "srch_destination_id",
          "hotel_cluster",
          "is_booking"
  };

  private static final String[] META = new String[]{
          "day-ctr", "CTR built on days",
          "user-ctr", "CTR built on users",
          "user-city-ctr", "CTR built on users' cities",
          "ci-ctr", "CTR built on checkin dates",
          "co-ctr", "CTR built on checkout dates",
          "dest-ctr", "CTR built on destinations",
          "hotel-ctr", "CTR built on hotels",
          "booked", "If the user booked the hotel"
  };

  private static final int BUILDERS_COUNT = 7;

  public static void build(final String trainPath, final String poolPath, final String builderPath) throws IOException {
    final ArrayList<CTRBuilder<Integer>> builders = new ArrayList<>();
    final VecBuilder booked = new VecBuilder();

    LOG.debug("Create features...");
    for (int i = 0; i < BUILDERS_COUNT; ++i) {
      builders.add(new CTRBuilder<>(META[2 * i], META[2 * i + 1]));
    }

    LOG.debug("Process data...");
    final PoolBuilder builder = new PoolBuilder();
    DataTools.readCSVWithHeader(trainPath, new Consumer<CsvRow>() {
      private int index = 0;
      private int[] values = new int[COLUMNS.length];

      @Override
      public void accept(final CsvRow row) {
        for (int i = 0; i < COLUMNS.length; ++i) {
          values[i] = row.asInt(COLUMNS[i]);
        }

        final int hasBooked = values[COLUMNS.length - 1];

        for (int i = 0; i < BUILDERS_COUNT; ++i) {
          builders.get(i).add(values[i], hasBooked > 0);
        }

        // add (day, user, hotel)
        builder.addItem(new EventItem(values[0], values[1], values[6]));
        booked.append(hasBooked);

        if (++index % 1_000_000 == 0) {
          System.out.println("Processed: " + index);
        }
      }
    });

    final JsonDataSetMeta dataSetMeta = new JsonDataSetMeta("Expedia",
            System.getProperty("user.name"),
            new Date(),
            Deal.class,
            "expedia-" + DateFormat.getInstance().format(new Date())
    );
    builder.setMeta(dataSetMeta);

    // add new features
    for (final CTRBuilder<Integer> ctr : builders) {
      builder.newFeature(ctr.getMeta(), ctr.build());
    }

    // add booked feature
    final JsonFeatureMeta bookedMeta = new JsonFeatureMeta();
    bookedMeta.id = META[2 * BUILDERS_COUNT];
    bookedMeta.description = META[2 * BUILDERS_COUNT + 1];
    bookedMeta.type = FeatureMeta.ValueType.VEC;
    builder.newFeature(bookedMeta, booked.build());

    // save builders
    LOG.debug("Save builders...");
    for (final CTRBuilder<Integer> ctr : builders) {
      ctr.write(builderPath);
    }

    // save data pool
    LOG.debug("Save pool...");
    try (final Writer out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(poolPath)))) {
      DataTools.writePoolTo(builder.create(), out);
    }
  }
}