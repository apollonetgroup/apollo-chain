package stest.apollo.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
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
public class WalletTestAssetIssue011 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final String updateMostLongName = Long.toString(now) + "w234567890123456789";
  private static String name = "testAssetIssue011_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 10000L;
  Long publicFreeAssetNetLimit = 10000L;
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset011Address = ecKey1.getAddress();
  String testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetCreateAddress = ecKey2.getAddress();
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
    PublicMethed.printAddress(testKeyForAssetIssue011);
    PublicMethed.printAddress(transferAssetCreateKey);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Transfer asset to create account")
  public void testTransferAssetCreateAccount() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset011Address = ecKey1.getAddress();
    testKeyForAssetIssue011 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetCreateAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed
        .sendcoin(asset011Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .freezeBalance(asset011Address, 100000000L, 3, testKeyForAssetIssue011,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed
        .createAssetIssue(asset011Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue011,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset011Address, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    //Transfer asset to create an account.
    Assert.assertTrue(PublicMethed
        .transferAsset(transferAssetCreateAddress, assetAccountId.toByteArray(), 1L,
            asset011Address, testKeyForAssetIssue011, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account queryTransferAssetAccount = PublicMethed
        .queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertTrue(queryTransferAssetAccount.getAssetV2Count() == 1);
    Assert.assertTrue(PublicMethed.updateAccount(asset011Address, Long.toString(now)
        .getBytes(), testKeyForAssetIssue011, blockingStubFull));
    Assert.assertTrue(PublicMethed.updateAccount(transferAssetCreateAddress, updateMostLongName
        .getBytes(), transferAssetCreateKey, blockingStubFull));
    queryTransferAssetAccount = PublicMethed.queryAccount(transferAssetCreateKey, blockingStubFull);
    Assert.assertFalse(queryTransferAssetAccount.getAccountName().isEmpty());
    PublicMethed
        .freedResource(asset011Address, testKeyForAssetIssue011, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(asset011Address, testKeyForAssetIssue011, 0, asset011Address,
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


