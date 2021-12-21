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
import org.junit.Test;
import org.testng.Assert;

@Slf4j
public class EnergyWhenSendAndTransferTest {

  private Manager dbManager;
  private ApolloApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_EnergyWhenSendAndTransferTest";
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

  // solidity for callValueTest
  // pragma solidity ^0.4.0;
  //
  // contract SubContract {
  //
  //   constructor () payable {}
  //   mapping(uint256=>uint256) map;
  //
  //   function doSimple() public payable returns (uint ret) {
  //     return 42;
  //   }
  //
  //   function doComplex() public payable returns (uint ret) {
  //     for (uint i = 0; i < 10; i++) {
  //       map[i] = i;
  //     }
  //   }
  //
  // }
  //
  // contract TestForValueGasFunction {
  //
  //   SubContract subContract;
  //
  //   constructor () payable {
  //     subContract = new SubContract();
  //   }
  //
  //   function simpleCall() public { subContract.doSimple.value(10).gas(3)(); }
  //
  //   function complexCall() public { subContract.doComplex.value(10).gas(3)(); }
  //
  // }

  @Test
  public void callValueTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {

    long value = 10000000L;
    long feeLimit = 1000_000_000L; // sun
    long consumeUserResourcePercent = 100;
    byte[] address = Hex.decode(OWNER_ADDRESS);
    TVMTestResult result = deployCallValueTestContract(value, feeLimit, consumeUserResourcePercent);

    long expectEnergyUsageTotal = 174639;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
    byte[] contractAddress = result.getContractAddress();
    Assert.assertEquals(deposit.getAccount(contractAddress).getBalance(), value);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - expectEnergyUsageTotal * 100);

    /* =================================== CALL simpleCall() =================================== */
    byte[] triggerData = TvmTestUtils.parseAbi("simpleCall()", null);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, 0, feeLimit, dbManager, null);

    long expectEnergyUsageTotal2 = 7370;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal2);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - (expectEnergyUsageTotal + expectEnergyUsageTotal2) * 100);

    /* =================================== CALL complexCall() =================================== */
    triggerData = TvmTestUtils.parseAbi("complexCall()", null);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, 0, feeLimit, dbManager, null);

    long expectEnergyUsageTotal3 = 9459;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal3);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(), totalBalance - value
        - (expectEnergyUsageTotal + expectEnergyUsageTotal2 + expectEnergyUsageTotal3) * 100);
  }

  // solidity for sendTest and transferTest
  // pragma solidity ^0.4.0;
  //
  // contract SubContract {
  //
  //   constructor () payable {}
  //   mapping(uint256=>uint256) map;
  //
  //   function () payable {
  //     map[1] = 1;
  //   }
  // }
  //
  // contract TestForSendAndTransfer {
  //
  //   SubContract subContract;
  //
  //   constructor () payable {
  //     subContract = new SubContract();
  //   }
  //
  //
  //   function doSend() public { address(subContract).send(10000); }
  //
  //   function doTransfer() public { address(subContract).transfer(10000); }
  //
  //   function getBalance() public view returns(uint256 balance){
  //     balance = address(this).balance;
  //   }
  //
  // }


  @Test
  public void sendTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {

    long value = 1000L;
    long feeLimit = 1000_000_000L; // sun
    long consumeUserResourcePercent = 100;
    byte[] address = Hex.decode(OWNER_ADDRESS);
    TVMTestResult result = deploySendAndTransferTestContract(value, feeLimit,
        consumeUserResourcePercent);

    long expectEnergyUsageTotal = 140194;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
    byte[] contractAddress = result.getContractAddress();
    Assert.assertEquals(deposit.getAccount(contractAddress).getBalance(), value);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - expectEnergyUsageTotal * 100);

    /* =================================== CALL doSend() =================================== */
    byte[] triggerData = TvmTestUtils.parseAbi("doSend()", null);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, 0, feeLimit, dbManager, null);

    long expectEnergyUsageTotal2 = 7025;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertEquals(deposit.getAccount(contractAddress).getBalance(), value);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - (expectEnergyUsageTotal + expectEnergyUsageTotal2) * 100);
  }

  @Test
  public void transferTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {

    long value = 1000L;
    // long value = 10000000L;
    long feeLimit = 1000_000_000L; // sun
    long consumeUserResourcePercent = 100;
    byte[] address = Hex.decode(OWNER_ADDRESS);
    TVMTestResult result = deploySendAndTransferTestContract(value, feeLimit,
        consumeUserResourcePercent);

    long expectEnergyUsageTotal = 140194;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal);
    byte[] contractAddress = result.getContractAddress();
    Assert.assertEquals(deposit.getAccount(contractAddress).getBalance(), value);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - expectEnergyUsageTotal * 100);

    /* =================================== CALL doSend() =================================== */
    byte[] triggerData = TvmTestUtils.parseAbi("doTransfer()", null);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, 0, feeLimit, dbManager, null);

    long expectEnergyUsageTotal2 = 7030;
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), expectEnergyUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertEquals(deposit.getAccount(contractAddress).getBalance(), value);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - value - (expectEnergyUsageTotal + expectEnergyUsageTotal2) * 100);
  }

  public TVMTestResult deployCallValueTestContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "TestForCallValue";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"complexCall\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":false,\"inputs\":[],\"name\":\"simpleCall\",\"outputs\":[],\"payable\""
        + ":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],"
        + "\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405261000c61004e565b604051809103906000f080158015610028573d6000803e3d6000f"
        + "d5b5060008054600160a060020a031916600160a060020a039290921691909117905561005d565b60405160"
        + "d68061020b83390190565b61019f8061006c6000396000f3006080604052600436106100325763ffffffff6"
        + "0e060020a60003504166306ce93af811461003757806340de221c1461004e575b600080fd5b348015610043"
        + "57600080fd5b5061004c610063565b005b34801561005a57600080fd5b5061004c610103565b60008090549"
        + "06101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffff"
        + "ffffffffffff1663cd95478c600a6003906040518363ffffffff1660e060020a02815260040160206040518"
        + "08303818589803b1580156100d357600080fd5b5088f11580156100e7573d6000803e3d6000fd5b50505050"
        + "50506040513d60208110156100ff57600080fd5b5050565b6000809054906101000a900473fffffffffffff"
        + "fffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663b993e5e2600a"
        + "6003906040518363ffffffff1660e060020a0281526004016020604051808303818589803b1580156100d35"
        + "7600080fd00a165627a7a72305820cb5f172ca9f81235a8b33ee1ddef9dd1b398644cf61228569356ff051b"
        + "faf3d10029608060405260c4806100126000396000f30060806040526004361060485763ffffffff7c01000"
        + "00000000000000000000000000000000000000000000000000000600035041663b993e5e28114604d578063"
        + "cd95478c146065575b600080fd5b6053606b565b60408051918252519081900360200190f35b60536070565"
        + "b602a90565b6000805b600a81101560945760008181526020819052604090208190556001016074565b5090"
        + "5600a165627a7a723058205ded543feb546472be4e116e713a2d46b8dafc823ca31256e67a1be92a6752730"
        + "029";
    String libraryAddressPair = null;

    return TvmTestUtils
        .deployContractAndReturnTvmTestResult(contractName, address, ABI, code, value, feeLimit,
            consumeUserResourcePercent, libraryAddressPair, dbManager, null);
  }

  public TVMTestResult deploySendAndTransferTestContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "TestForSendAndTransfer";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\""
        + ":\"balance\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\""
        + "type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"doTransfer\",\""
        + "outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\""
        + "},{\"constant\":false,\"inputs\":[],\"name\":\"doSend\",\"outputs\":[],\"payable\":"
        + "false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\""
        + "payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405261000c61004e565b604051809103906000f080158015610028573d6000803e3d6000f"
        + "d5b5060008054600160a060020a031916600160a060020a039290921691909117905561005d565b60405160"
        + "6f806101c583390190565b6101598061006c6000396000f3006080604052600436106100565763ffffffff7"
        + "c010000000000000000000000000000000000000000000000000000000060003504166312065fe081146100"
        + "5b57806333182e8f14610082578063e3d237f914610099575b600080fd5b34801561006757600080fd5b506"
        + "100706100ae565b60408051918252519081900360200190f35b34801561008e57600080fd5b506100976100"
        + "b3565b005b3480156100a557600080fd5b506100976100f9565b303190565b6000805460405173fffffffff"
        + "fffffffffffffffffffffffffffffff90911691906127109082818181858883f193505050501580156100f6"
        + "573d6000803e3d6000fd5b50565b6000805460405173ffffffffffffffffffffffffffffffffffffffff909"
        + "11691906127109082818181858883f150505050505600a165627a7a72305820677efa58ed7b277b589fe662"
        + "6cb77f930caeb0f75c3ab638bfe07292db961a8200296080604052605e8060116000396000f300608060405"
        + "2600160008181526020527fada5013122d395ba3c54772283fb069b10426056ef8ca54750cb9bb552a59e7d"
        + "550000a165627a7a7230582029b27c10c1568d590fa66bc0b7d42537a314c78d028f59a188fa411f7fc15c4"
        + "f0029";
    String libraryAddressPair = null;

    return TvmTestUtils
        .deployContractAndReturnTvmTestResult(contractName, address, ABI, code, value, feeLimit,
            consumeUserResourcePercent, libraryAddressPair, dbManager, null);
  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    context.destroy();
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.warn("Release resources failure.");
    }
  }

}
