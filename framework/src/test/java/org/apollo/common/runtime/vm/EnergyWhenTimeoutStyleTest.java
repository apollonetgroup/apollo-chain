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
import org.apollo.core.vm.program.Program.OutOfEnergyException;
import org.apollo.core.vm.program.Program.OutOfTimeException;
import org.apollo.protos.Protocol.AccountType;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

@Slf4j
public class EnergyWhenTimeoutStyleTest {

  private Manager dbManager;
  private ApolloApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_CPUTimeTest";
  private String OWNER_ADDRESS;
  private Application AppT;
  private long totalBalance = 30_000_000_000_000L;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), totalBalance);
    deposit.commit();
  }

  // solidity for endlessLoopTest
  // pragma solidity ^0.4.0;
  //
  // contract TestForEndlessLoop {
  //
  //   uint256 vote;
  //   constructor () public {
  //     vote = 0;
  //   }
  //
  //   function getVote() public constant returns (uint256 _vote) {
  //     _vote = vote;
  //   }
  //
  //   function setVote(uint256 _vote) public {
  //     vote = _vote;
  //     while(true)
  //     {
  //       vote += 1;
  //     }
  //   }
  // }

  @Test
  public void endlessLoopTest()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException,
      VMIllegalException {

    long value = 0;
    long feeLimit = 1000_000_000L;
    byte[] address = Hex.decode(OWNER_ADDRESS);
    long consumeUserResourcePercent = 0;
    TVMTestResult result = deployEndlessLoopContract(value, feeLimit,
        consumeUserResourcePercent);

    if (null != result.getRuntime().getResult().getException()) {
      long expectEnergyUsageTotal = feeLimit / 100;
      Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
      Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
          totalBalance - expectEnergyUsageTotal * 100);
      return;
    }
    long expectEnergyUsageTotal = 55107;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectEnergyUsageTotal * 100);

    byte[] contractAddress = result.getContractAddress();

    /* =================================== CALL setVote(uint256) =============================== */
    String params = "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = TvmTestUtils.parseAbi("setVote(uint256)", params);
    boolean haveException = false;
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, value, feeLimit, dbManager, null);

    long expectEnergyUsageTotal2 = feeLimit / 100;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal2);
    Exception exception = result.getRuntime().getResult().getException();
    Assert.assertTrue((exception instanceof OutOfTimeException)
        || (exception instanceof OutOfEnergyException));
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectEnergyUsageTotal + expectEnergyUsageTotal2) * 100);
  }

  public TVMTestResult deployEndlessLoopContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "EndlessLoopContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"getVote\",\"outputs\":[{\"name\""
        + ":\"_vote\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_vote\",\"type\":"
        + "\"uint256\"}],\"name\":\"setVote\",\"outputs\":[],\"payable\":false,\"stateMutability\""
        + ":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";

    String code = "608060405234801561001057600080fd5b506000808190555060fa806100266000396000f3006080"
        + "604052600436106049576000357c010000000000000000000000000000000000000000000000000000000090"
        + "0463ffffffff1680630242f35114604e578063230796ae146076575b600080fd5b348015605957600080fd5b"
        + "50606060a0565b6040518082815260200191505060405180910390f35b348015608157600080fd5b50609e60"
        + "04803603810190808035906020019092919050505060a9565b005b60008054905090565b806000819055505b"
        + "60011560cb576001600080828254019250508190555060b1565b505600a165627a7a72305820290a38c9bbaf"
        + "ccaf6c7f752ab56d229e354da767efb72715ee9fdb653b9f4b6c0029";

    String libraryAddressPair = null;

    return TvmTestUtils
        .deployContractAndReturnTvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);
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
