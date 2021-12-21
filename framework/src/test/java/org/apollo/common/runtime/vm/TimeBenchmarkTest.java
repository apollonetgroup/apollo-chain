package org.apollo.common.runtime.vm;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.runtime.TVMTestResult;
import org.apollo.common.runtime.TvmTestUtils;
import org.apollo.common.storage.DepositImpl;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;
import org.apollo.core.exception.ReceiptCheckErrException;
import org.apollo.core.exception.VMIllegalException;
import org.apollo.protos.Protocol.AccountType;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.testng.Assert;

@Slf4j
@Ignore
public class TimeBenchmarkTest {

  private Manager dbManager;
  private ApolloApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_TimeBenchmarkTest";
  private String OWNER_ADDRESS;
  private Application AppT;
  private long totalBalance = 30_000_000_000_000L;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), totalBalance);
    deposit.commit();
  }

  // pragma solidity ^0.4.2;
  //
  // contract Fibonacci {
  //
  //   event Notify(uint input, uint result);
  //
  //   function fibonacci(uint number) constant returns(uint result) {
  //     if (number == 0) {
  //       return 0;
  //     }
  //     else if (number == 1) {
  //       return 1;
  //     }
  //     else {
  //       uint256 first = 0;
  //       uint256 second = 1;
  //       uint256 ret = 0;
  //       for(uint256 i = 2; i <= number; i++) {
  //         ret = first + second;
  //         first = second;
  //         second = ret;
  //       }
  //       return ret;
  //     }
  //   }
  //
  //   function fibonacciNotify(uint number) returns(uint result) {
  //     result = fibonacci(number);
  //     Notify(number, result);
  //   }
  // }

  @Test
  public void timeBenchmark()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException,
      VMIllegalException {
    long value = 0;
    long feeLimit = 200_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "timeBenchmark";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],"
        + "\"name\":\"fibonacciNotify\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":true,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"fibonacci\""
        + ",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":"
        + "[{\"indexed\":false,\"name\":\"input\",\"type\":\"uint256\"},{\"indexed\":false,\"name"
        + "\":\"result\",\"type\":\"uint256\"}],\"name\":\"Notify\",\"type\":\"event\"}]";
    String code = "608060405234801561001057600080fd5b506101ba806100206000396000f300608060405260043"
        + "61061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffff"
        + "ffff1680633c7fdc701461005157806361047ff414610092575b600080fd5b34801561005d57600080fd5b5"
        + "061007c600480360381019080803590602001909291905050506100d3565b60405180828152602001915050"
        + "60405180910390f35b34801561009e57600080fd5b506100bd6004803603810190808035906020019092919"
        + "0505050610124565b6040518082815260200191505060405180910390f35b60006100de82610124565b9050"
        + "7f71e71a8458267085d5ab16980fd5f114d2d37f232479c245d523ce8d23ca40ed828260405180838152602"
        + "0018281526020019250505060405180910390a1919050565b60008060008060008086141561013d57600094"
        + "50610185565b600186141561014f5760019450610185565b600093506001925060009150600290505b85811"
        + "115156101815782840191508293508192508080600101915050610160565b8194505b505050509190505600"
        + "a165627a7a72305820637e163344c180cd57f4b3a01b07a5267ad54811a5a2858b5d67330a2724ee680029";
    String libraryAddressPair = null;

    TVMTestResult result = TvmTestUtils
        .deployContractAndReturnTvmTestResult(contractName, address, ABI, code, value, feeLimit,
            consumeUserResourcePercent, libraryAddressPair, dbManager, null);

    long expectEnergyUsageTotal = 88529;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectEnergyUsageTotal * 100);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TvmTestUtils.parseAbi("fibonacciNotify(uint)", "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, 0, feeLimit, dbManager, null);

    long expectEnergyUsageTotal2 = 110;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(result.getRuntime().getResult().getException() == null);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectEnergyUsageTotal + expectEnergyUsageTotal2) * 100);
  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}
