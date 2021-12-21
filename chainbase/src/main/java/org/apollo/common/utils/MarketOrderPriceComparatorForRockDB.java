package org.apollo.common.utils;

import org.apollo.core.capsule.utils.MarketUtils;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.DirectSlice;
import org.rocksdb.util.DirectBytewiseComparator;

public class MarketOrderPriceComparatorForRockDB extends DirectBytewiseComparator {

  public MarketOrderPriceComparatorForRockDB(final ComparatorOptions copt) {
    super(copt);
  }

  @Override
  public String name() {
    return "MarketOrderPriceComparator";
  }

  @Override
  public int compare(final DirectSlice a, final DirectSlice b) {
    return MarketUtils.comparePriceKey(convertDataToBytes(a), convertDataToBytes(b));
  }

  /**
   * DirectSlice.data().array will throw UnsupportedOperationException.
   * */
  public byte[] convertDataToBytes(DirectSlice directSlice) {
    int capacity = directSlice.data().capacity();
    byte[] bytes = new byte[capacity];

    for (int i = 0; i < capacity; i++) {
      bytes[i] = directSlice.get(i);
    }

    return bytes;
  }

}
