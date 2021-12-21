package stest.apollo.wallet.dailybuild.tvmnewcommand.batchValidateSignContract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.TransactionInfo;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class batchValidateSignContract009 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String txid = "";
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    txid = PublicMethed
        .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
            testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/batchvalidatesign001.sol";
    String contractName = "Demo";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Incorrect address hex test multivalidatesign")
  public void test02IncorrectAddressHex() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String input = "7d889f42b4a56ebe78264631a3b4daf21019e1170cce71929fb396761cdf532e00000000000000"
        + "000000000000000000000000000000000000000000000000600000000000000000000000000000000000000"
        + "0000000000000000000000001c0000000000000000000000000000000000000000000000000000000000000"
        + "000200000000000000000000000000000000000000000000000000000000000000400000000000000000000"
        + "0000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000"
        + "0000000000000000000041ad7ca8100cf0ce028b83ac719c8458655a6605317abfd071b91f5cc14d53e87a2"
        + "99fe0cdf6a8567074e9be3944affba33b1e15d14b7cb9003ec2c87cb1a56405000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000417ce31e565fb99451f87db65e75f46672e8a8f7b29e6589e60fd11e076550d0a66d0b05"
        + "e4b4d7d40bd34140f13dc3632d3ce0f25e4cf75840238b6fe2346c94fa01000000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "0000000000020000000000000000000000410d6b1de9e84c1d7a9a5b43d93dbe4a5aae79b18900000000000"
        + "00000000000123456";
    txid = PublicMethed
        .triggerContractBoth(contractAddress, "testArray(bytes32,bytes[],address[])", input, true,
            0,
            maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull, blockingStubFull1);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, infoById.get().getResultValue());
    Assert.assertTrue(
        infoById.get().getResMessage().toStringUtf8().contains("REVERT opcode executed"));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "Empty address and signatures hex test multivalidatesign")
  public void test03EmptyAddressAndSignaturesHex() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String input = "da586d881362c0c38eb31b556ce0f7c2837a3ebb60080e8e665a6b92c7541837b95064ba000000"
        + "000000000000000000000000000000000000000000000000000000006000000000000000000000000000000"
        + "000000000000000000000000000000001200000000000000000000000000000000000000000000000000000"
        + "000000000001000000000000000000000000000000000000000000000000000000000000002000000000000"
        + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000000001000000000000000000000000000000000000000"
        + "0000000000000000000000000";
    txid = PublicMethed
        .triggerContractBoth(contractAddress, "testArray(bytes32,bytes[],address[])", input, true,
            0,
            maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull, blockingStubFull1);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, infoById.get().getResultValue());
    Assert.assertTrue(
        infoById.get().getResMessage().toStringUtf8().contains("REVERT opcode executed"));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethed.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
