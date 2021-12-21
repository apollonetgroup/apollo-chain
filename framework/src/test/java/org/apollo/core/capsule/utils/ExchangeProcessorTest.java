package org.apollo.core.capsule.utils;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.capsule.ExchangeProcessor;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class ExchangeProcessorTest {

  private static final String dbPath = "output_buy_exchange_processor_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000_000_000L;
  private static ExchangeProcessor processor;
  private static ApolloApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    long supply = 1_000_000_000_000_000_000L;
    processor = new ExchangeProcessor(supply);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testExchange() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);

    Assert.assertEquals(2694881440L, result);
  }

  @Test
  public void testExchange2() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 1_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(1360781717L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long result2 = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L - 1360781717L, result2);

  }


  @Test
  public void testSellAndBuy() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long result2 = processor.exchange(buyBalance, sellBalance, result);
    Assert.assertEquals(1999999999542L, result2);

  }

  @Test
  public void testSellAndBuy2() {
    long sellBalance = 100_000_000_000000L;
    long buyBalance = 128L * 1024 * 1024 * 1024;
    long sellQuant = 2_000_000_000_000L; // 2 million trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(2694881440L, result);

    sellBalance += sellQuant;
    buyBalance -= result;

    long quant1 = 2694881440L - 1360781717L;
    long quant2 = 1360781717L;

    long result1 = processor.exchange(buyBalance, sellBalance, quant1);
    Assert.assertEquals(999999999927L, result1);

    buyBalance += quant1;
    sellBalance -= result1;

    long result2 = processor.exchange(buyBalance, sellBalance, quant2);
    Assert.assertEquals(999999999587L, result2);

  }

  @Test
  public void testInject() {
    long sellBalance = 1_000_000_000000L;
    long buyBalance = 10_000_000L;
    long sellQuant = 10_000_000L; // 10 trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(99L, result);

    // inject
    sellBalance += 100_000_000000L;
    buyBalance += 1_000_000L;

    long result2 = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(99L, result2);

  }

  @Test
  public void testWithdraw() {
    long sellBalance = 1_000_000_000000L;
    long buyBalance = 10_000_000L;
    long sellQuant = 10_000_000L; // 10 trx

    long result = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(99L, result);

    // inject
    sellBalance -= 800_000_000000L;
    buyBalance -= 8_000_000L;

    long result2 = processor.exchange(sellBalance, buyBalance, sellQuant);
    Assert.assertEquals(99L, result2);

  }


}
