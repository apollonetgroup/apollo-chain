package org.apollo.common.utils;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.MarketOrderPriceComparatorForLevelDB;
import org.apollo.core.capsule.utils.MarketUtils;
import org.junit.Assert;
import org.junit.Test;


@Slf4j
public class DBKeyComparatorTest {


  @Test
  public void dbComparing() {
    MarketOrderPriceComparatorForLevelDB comparator = new MarketOrderPriceComparatorForLevelDB();

    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        ByteArray.fromString("100"),
        ByteArray.fromString("200"),
        1000L,
        2000L
    );
    byte[] pairPriceKey2 = MarketUtils.createPairPriceKey(
        ByteArray.fromString("100"),
        ByteArray.fromString("200"),
        1000L,
        2001L
    );
    Assert.assertEquals(-1, comparator.compare(pairPriceKey1, pairPriceKey2));
  }


  @Test
  public void pairKeyIsEqual() {

    byte[] pairPriceKey1 = MarketUtils.createPairPriceKey(
        ByteArray.fromString("100"),
        ByteArray.fromString("200"),
        1000L,
        2000L
    );
    byte[] pairPriceKey2 = MarketUtils.createPairPriceKey(
        ByteArray.fromString("10"),
        ByteArray.fromString("200"),
        1000L,
        2001L
    );

    Assert.assertFalse(MarketUtils.pairKeyIsEqual(pairPriceKey1, pairPriceKey2));
  }



}
