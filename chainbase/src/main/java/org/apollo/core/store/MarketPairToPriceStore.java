package org.apollo.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.capsule.BytesCapsule;
import org.apollo.core.capsule.MarketOrderIdListCapsule;
import org.apollo.core.capsule.utils.MarketUtils;
import org.apollo.core.db.TronStoreWithRevoking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This store is used to store the first price Key of specific token pair
 * Key: sell_id + buy_id, use createPairKey
 * Value: sell_id + buy_id + sell_quantity + buy_quantity, use createPairPriceKey
 * */
@Component
public class MarketPairToPriceStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  protected MarketPairToPriceStore(@Value("market_pair_to_price") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public long getPriceNum(byte[] key) {
    BytesCapsule bytesCapsule = get(key);
    if (bytesCapsule != null) {
      return ByteArray.toLong(bytesCapsule.getData());
    } else {
      return 0L;
    }
  }

  public long getPriceNum(byte[] sellTokenId, byte[] buyTokenId) {
    return getPriceNum(MarketUtils.createPairKey(sellTokenId, buyTokenId));
  }

  public void setPriceNum(byte[] key, long number) {
    put(key, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public void setPriceNum(byte[] sellTokenId, byte[] buyTokenId, long number) {
    setPriceNum(MarketUtils.createPairKey(sellTokenId, buyTokenId), number);
  }

  /**
   * if pair not exits, add token pair, set count = 1, add headKey to pairPriceToOrderStore.
   * otherwise, increase count
   * */
  public void addNewPriceKey(byte[] sellTokenId, byte[] buyTokenId,
      MarketPairPriceToOrderStore pairPriceToOrderStore) {
    long number;

    byte[] pairKey = MarketUtils.createPairKey(sellTokenId, buyTokenId);
    if (has(pairKey)) {
      number = getPriceNum(pairKey) + 1;
    } else {
      number = 1;
      byte[] headKey = MarketUtils.getPairPriceHeadKey(sellTokenId, buyTokenId);
      pairPriceToOrderStore.put(headKey, new MarketOrderIdListCapsule());
    }

    setPriceNum(pairKey, number);
  }
}