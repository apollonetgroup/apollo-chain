package org.apollo.core.actuator.utils;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.Constant;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.utils.ZenChainParams;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


@Slf4j(topic = "capsule")
public class ZenChainParamsTest {

  private static final String dbPath = "output_zenchainparams_test";
  public static Application AppT;
  private static ApolloApplicationContext context;

  /**
   * Init .
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
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
  public void variableCheck() {
    ZenChainParams actuatorUtils = new ZenChainParams();
    Assert.assertEquals(16, actuatorUtils.NOTEENCRYPTION_AUTH_BYTES);
    Assert.assertEquals(1, actuatorUtils.ZC_NOTEPLAINTEXT_LEADING);
    Assert.assertEquals(8, actuatorUtils.ZC_V_SIZE);
    Assert.assertEquals(32, actuatorUtils.ZC_R_SIZE);
    Assert.assertEquals(512, actuatorUtils.ZC_MEMO_SIZE);
    Assert.assertEquals(11, actuatorUtils.ZC_DIVERSIFIER_SIZE);
    Assert.assertEquals(32, actuatorUtils.ZC_JUBJUB_POINT_SIZE);
    Assert.assertEquals(32, actuatorUtils.ZC_JUBJUB_SCALAR_SIZE);
    int ZC_ENCPLAINTEXT_SIZE =
        actuatorUtils.ZC_NOTEPLAINTEXT_LEADING + actuatorUtils.ZC_DIVERSIFIER_SIZE
            + actuatorUtils.ZC_V_SIZE + actuatorUtils.ZC_R_SIZE + actuatorUtils.ZC_MEMO_SIZE;
    Assert.assertEquals(ZC_ENCPLAINTEXT_SIZE, actuatorUtils.ZC_ENCPLAINTEXT_SIZE);
    int ZC_ENCCIPHERTEXT_SIZE = (actuatorUtils.ZC_ENCPLAINTEXT_SIZE
        + actuatorUtils.NOTEENCRYPTION_AUTH_BYTES);
    Assert.assertEquals(ZC_ENCCIPHERTEXT_SIZE, actuatorUtils.ZC_ENCCIPHERTEXT_SIZE);
    int ZC_OUTCIPHERTEXT_SIZE = (actuatorUtils.ZC_OUTPLAINTEXT_SIZE
        + actuatorUtils.NOTEENCRYPTION_AUTH_BYTES);
    Assert.assertEquals(ZC_OUTCIPHERTEXT_SIZE, actuatorUtils.ZC_OUTCIPHERTEXT_SIZE);
    Assert.assertTrue(actuatorUtils instanceof ZenChainParams);
  }

}
