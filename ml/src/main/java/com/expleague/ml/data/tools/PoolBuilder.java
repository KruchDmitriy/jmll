package com.expleague.ml.data.tools;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


import com.expleague.commons.seq.ArraySeq;
import com.expleague.commons.func.Factory;
import com.expleague.commons.seq.Seq;
import com.expleague.commons.util.Pair;
import com.expleague.ml.meta.DSItem;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.ml.meta.impl.JsonFeatureMeta;
import com.expleague.ml.meta.impl.JsonTargetMeta;

/**
 * User: solar
 * Date: 07.07.14
 * Time: 12:55
 */
@SuppressWarnings("unchecked")
public class PoolBuilder implements Factory<Pool<? extends DSItem>> {
  private JsonDataSetMeta meta;
  private List<DSItem> items = new ArrayList<>();
  private List<Pair<JsonFeatureMeta, Seq<?>>> features = new ArrayList<>();
  private List<Pair<JsonTargetMeta, Seq<?>>> targets = new ArrayList<>();

  @Override
  public Pool<? extends DSItem> create() {
    return create((Class<DSItem>)meta.type());
  }

  public <Item extends DSItem> Pool<Item> create(final Class<Item> clazz) {
    final Pool<Item> result = new Pool<>(
        meta,
        new ArraySeq<>(items.toArray((Item[])Array.newInstance(items.get(0).getClass(), items.size()))),
        features.toArray((Pair<JsonFeatureMeta, Seq<?>>[]) new Pair[features.size()]),
        targets.toArray((Pair<JsonTargetMeta, Seq<?>>[]) new Pair[targets.size()]));
    { // verifying lines
      for (final Pair<JsonFeatureMeta, Seq<?>> entry : features) {
        entry.getFirst().owner = result;
        if (entry.second.length() != items.size())
          throw new RuntimeException(
              "Feature " + entry.first.toString() + " has " + entry.second.length() + " entries " + " expected " + items.size());
      }
    }
    { // checking targets
      for (final Pair<JsonTargetMeta, Seq<?>> entry : targets) {
        entry.getFirst().owner = result;
        if (entry.second.length() != items.size())
          throw new RuntimeException(
              "Target has " + entry.second.length() + " entries " + " expected " + items.size());
      }
    }

    final Set<String> itemIds = new HashSet<>();
    for (final Item item : (List<Item>)items) {
      if (itemIds.contains(item.id()))
        throw new RuntimeException(
            "Contain duplicates! Id = " + item.id()
        );
      itemIds.add(toString());
    }
    meta = null;
    items = new ArrayList<>();
    features = new ArrayList<>();
    targets = new ArrayList<>();
    return result;
  }

  public void setMeta(final JsonDataSetMeta meta) {
    this.meta = meta;
  }

  public void addItem(final DSItem read) {
    items.add(read);
  }

  public void newFeature(final JsonFeatureMeta meta, final Seq<?> values) {
    meta.associated = this.meta.id();
    features.add(Pair.<JsonFeatureMeta, Seq<?>>create(meta, values));
  }

  public void newTarget(final JsonTargetMeta meta, final Seq<?> target) {
    meta.associated = this.meta.id();
    this.targets.add(Pair.<JsonTargetMeta, Seq<?>>create(meta, target));
  }

  public <Item extends DSItem> Stream<Item> items() {
    return (Stream<Item>) items.stream();
  }
}
