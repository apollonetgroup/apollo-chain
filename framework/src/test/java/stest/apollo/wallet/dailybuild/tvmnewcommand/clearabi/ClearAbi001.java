package stest.apollo.wallet.dailybuild.tvmnewcommand.clearabi;

import static org.hamcrest.core.StringContains.containsString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.AccountResourceMessage;
import org.apollo.api.GrpcAPI.TransactionExtention;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.TransactionInfo;
import org.apollo.protos.contract.SmartContractOuterClass.SmartContract;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ClearAbi001 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress1 = ecKey2.getAddress();
  String contractExcKey1 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress1, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Clear a contract created by other account")
  public void testClearAbi001() {
    String filePath = "src/test/resources/soliditycode/ClearAbi001.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    TransactionExtention transactionExtention = PublicMethed
        .clearContractAbiForExtention(contractAddress, contractExcAddress1, contractExcKey1,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("is not the owner of the contract"));

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());


  }


  @Test(enabled = true, description = "Clear a contract with ABI created by itself")
  public void testClearAbi002() {

    String contractName = "testConstantContract";
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = PublicMethed
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    String txid1 = PublicMethed
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }


  @Test(enabled = true, description = "Clear a contract without ABI")
  public void testClearAbi003() {

    String contractName = "testConstantContract";
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String txid = PublicMethed
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());


  }

  @Test(enabled = true, description = "Clear a account address")
  public void testClearAbi004() {
    TransactionExtention transactionExtention = PublicMethed
        .clearContractAbiForExtention(contractExcAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("contract validate error : Contract not exists"));
  }


  @Test(enabled = true, description = "Clear a uninitialized account")
  public void testClearAbi005() {

    ECKey ecKeyN = new ECKey(Utils.getRandom());
    byte[] contractExcAddressN = ecKeyN.getAddress();
    String contractExcKeyN = ByteArray.toHexString(ecKeyN.getPrivKeyBytes());

    TransactionExtention transactionExtention = PublicMethed
        .clearContractAbiForExtention(contractExcAddressN, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_VALIDATE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("contract validate error : Contract not exists"));

  }

  @Test(enabled = true, description = "Clear a not meet the rules address")
  public void testClearAbi006() {
    byte[] fakeAddress = "412B5D".getBytes();
    TransactionExtention transactionExtention = PublicMethed
        .clearContractAbiForExtention(fakeAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("contract validate error : Contract not exists"));
    byte[] fakeAddress1 = "412B5D3405B2D26767C9C09886D53DEAFF6EB718AC111".getBytes();

    TransactionExtention transactionExtention1 = PublicMethed
        .clearContractAbiForExtention(fakeAddress1, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention1.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention1.getResult().getMessage().toStringUtf8(),
            containsString("contract validate error : Contract not exists"));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    PublicMethed.freedResource(contractExcAddress1, contractExcKey1, testNetAccountAddress,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
