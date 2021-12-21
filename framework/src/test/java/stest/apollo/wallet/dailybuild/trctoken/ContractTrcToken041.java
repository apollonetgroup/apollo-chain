package stest.apollo.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AccountResourceMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.TransactionInfo;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.Base58;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken041 {


  private static final long TotalSupply = 10000000L;
  private static final long now = System.currentTimeMillis();
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

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

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true, description = "Trigger contract msg.tokenId add 1,msg.tokenValue add 1")
  public void deployTransferTokenContract() {
    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 2048000000,
            fromAddress, testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 4048000000L,
            fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    // freeze balance
    PublicMethed.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull);

    PublicMethed.freezeBalanceGetEnergy(user001Address, 2048000000,
        0, 1, user001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        100, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    assetAccountId = PublicMethed.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();

    final ByteString fakeTokenId = ByteString
        .copyFromUtf8(Long.toString(Long.valueOf(assetAccountId.toStringUtf8()) + 100));

    // deploy transferTokenContract
    String filePath = "src/test/resources/soliditycode/contractTrcToken041.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, assetAccountId.toStringUtf8(),
            0, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(user001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeAssetIssueCount = PublicMethed
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
    Long beforeAssetIssueContractAddress = PublicMethed
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueCount);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);

    // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(dev001Address) + "\",\"" + fakeTokenId
            .toStringUtf8()
            + "\",\"105\"";

    final String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,trcToken,uint256)",
        param, false, 0, 100000000L, fakeTokenId.toStringUtf8(),
        10000000L, user001Address, user001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
            .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    Account infoafter = PublicMethed.queryAccount(user001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterAssetIssueCount = PublicMethed
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress = PublicMethed
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueCount);
    logger.info("afterAssetIssueContractAddress:" + afterAssetIssueContractAddress);

    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(beforeAssetIssueCount, afterAssetIssueCount);
    Assert.assertEquals(beforeAssetIssueContractAddress, afterAssetIssueContractAddress);
    Assert.assertTrue(afterEnergyUsed == 0L);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethed.freedResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(dev001Address, dev001Key, 1, dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(user001Address, user001Key, 1, user001Address, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}


