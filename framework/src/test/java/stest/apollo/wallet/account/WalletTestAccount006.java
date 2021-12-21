package stest.apollo.wallet.account;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
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
public class WalletTestAccount006 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 20000000000L;
  private static final long FREENETLIMIT = 5000L;
  private static final long BASELINE = 4800L;
  private static String name = "AssetIssue012_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] account006Address = ecKey.getAddress();
  String account006Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
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
    PublicMethed.printAddress(account006Key);

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void test1GetAccountNet() {
    ecKey = new ECKey(Utils.getRandom());
    account006Address = ecKey.getAddress();
    account006Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    //Sendcoin to this account
    ByteString addressBS1 = ByteString.copyFrom(account006Address);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    Assert.assertTrue(PublicMethed.freezeBalance(fromAddress, 100000000, 3, testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(account006Address, sendAmount, fromAddress, testKey002, blockingStubFull));

    //Get new account net information.
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    logger.info(Long.toString(accountNetMessage.getNetLimit()));
    logger.info(Long.toString(accountNetMessage.getNetUsed()));
    logger.info(Long.toString(accountNetMessage.getFreeNetLimit()));
    logger.info(Long.toString(accountNetMessage.getFreeNetUsed()));
    logger.info(Long.toString(accountNetMessage.getTotalNetLimit()));
    logger.info(Long.toString(accountNetMessage.getTotalNetWeight()));
    Assert.assertTrue(accountNetMessage.getNetLimit() == 0);
    Assert.assertTrue(accountNetMessage.getNetUsed() == 0);
    Assert.assertTrue(accountNetMessage.getFreeNetLimit() == FREENETLIMIT);
    Assert.assertTrue(accountNetMessage.getFreeNetUsed() == 0);
    Assert.assertTrue(accountNetMessage.getTotalNetLimit() > 0);
    Assert.assertTrue(accountNetMessage.getTotalNetWeight() > 0);
    logger.info("testGetAccountNet");

  }

  @Test(enabled = true)
  public void test2UseFreeNet() {

    //Transfer some TRX to other to test free net cost.
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    //Every transaction may cost 200 net.
    Assert.assertTrue(accountNetMessage.getFreeNetUsed() > 0 && accountNetMessage
        .getFreeNetUsed() < 300);
    logger.info("testUseFreeNet");
  }

  @Test(enabled = true)
  public void test3UseMoneyToDoTransaction() {
    Assert.assertTrue(PublicMethed.sendcoin(account006Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    //Use out the free net
    Integer times = 0;
    while (accountNetMessage.getFreeNetUsed() < BASELINE && times++ < 30) {
      PublicMethed.sendcoin(fromAddress, 1L, account006Address, account006Key,
          blockingStubFull);
      accountNetMessage = blockingStubFull.getAccountNet(request);
    }

    Account queryAccount = PublicMethed.queryAccount(account006Key, blockingStubFull);
    Long beforeSendBalance = queryAccount.getBalance();
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, 1L, account006Address, account006Key,
        blockingStubFull));
    queryAccount = PublicMethed.queryAccount(account006Key, blockingStubFull);
    Long afterSendBalance = queryAccount.getBalance();
    //when the free net is not enough and no balance freeze, use money to do the transaction.
    Assert.assertTrue(beforeSendBalance - afterSendBalance > 1);
    logger.info("testUseMoneyToDoTransaction");
  }

  @Test(enabled = true)
  public void test4UseNet() {
    //Freeze balance to own net.
    Assert.assertTrue(PublicMethed.freezeBalance(account006Address, 10000000L,
        3, account006Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(toAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    Assert.assertTrue(accountNetMessage.getNetLimit() > 0);
    Assert.assertTrue(accountNetMessage.getNetUsed() > 150);

    Account queryAccount = PublicMethed.queryAccount(account006Key, blockingStubFull);
    Long beforeSendBalance = queryAccount.getBalance();
    Assert.assertTrue(PublicMethed.sendcoin(fromAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    queryAccount = PublicMethed.queryAccount(account006Key, blockingStubFull);
    Long afterSendBalance = queryAccount.getBalance();
    //when you freeze balance and has net,you didn't cost money.
    logger.info("before is " + Long.toString(beforeSendBalance) + " and after is "
        + Long.toString(afterSendBalance));
    Assert.assertTrue(beforeSendBalance - afterSendBalance == 1);
    addressBs = ByteString.copyFrom(account006Address);
    request = Account.newBuilder().setAddress(addressBs).build();
    accountNetMessage = blockingStubFull.getAccountNet(request);
    //when you freeze balance and has net,you cost net.
    logger.info(Long.toString(accountNetMessage.getNetUsed()));
    Assert.assertTrue(accountNetMessage.getNetUsed() > 350);
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


