package stest.apollo.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AccountNetMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue014 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "AssetIssue014_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 3000L;
  Long publicFreeAssetNetLimit = 300L;
  String description = "for case assetissue014";
  String url = "https://stest.assetissue014.url";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset014Address = ecKey1.getAddress();
  String testKeyForAssetIssue014 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

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
    logger.info(testKeyForAssetIssue014);
    logger.info(transferAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Use transfer net when no enough public free asset net")
  public void testWhenNoEnoughPublicFreeAssetNetLimitUseTransferNet() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset014Address = ecKey1.getAddress();
    testKeyForAssetIssue014 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed
        .sendcoin(asset014Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset014Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue014,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset014Address, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    //Transfer asset to an account.
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .transferAsset(transferAssetAddress, assetAccountId.toByteArray(), 10000000L,
            asset014Address, testKeyForAssetIssue014, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Transfer send some asset issue to default account, to test if this
    // transaction use the creator net.
    Assert.assertTrue(PublicMethed.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Before use transfer net, query the net used from creator and transfer.
    AccountNetMessage assetCreatorNet = PublicMethed
        .getAccountNet(asset014Address, blockingStubFull);
    AccountNetMessage assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress, blockingStubFull);
    Long creatorBeforeNetUsed = assetCreatorNet.getNetUsed();
    Long transferBeforeFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorBeforeNetUsed));
    logger.info(Long.toString(transferBeforeFreeNetUsed));

    //Transfer send some asset issue to default account, to test if this
    // transaction use the transaction free net.
    Assert.assertTrue(PublicMethed.transferAsset(toAddress, assetAccountId.toByteArray(), 1L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetCreatorNet = PublicMethed
        .getAccountNet(asset014Address, blockingStubFull);
    assetTransferNet = PublicMethed
        .getAccountNet(transferAssetAddress, blockingStubFull);
    Long creatorAfterNetUsed = assetCreatorNet.getNetUsed();
    Long transferAfterFreeNetUsed = assetTransferNet.getFreeNetUsed();
    logger.info(Long.toString(creatorAfterNetUsed));
    logger.info(Long.toString(transferAfterFreeNetUsed));

    Assert.assertTrue(creatorAfterNetUsed - creatorBeforeNetUsed < netCostMeasure);
    Assert.assertTrue(transferAfterFreeNetUsed - transferBeforeFreeNetUsed > netCostMeasure);

    PublicMethed
        .freedResource(asset014Address, testKeyForAssetIssue014, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(asset014Address, testKeyForAssetIssue014, 0, asset014Address,
        blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


