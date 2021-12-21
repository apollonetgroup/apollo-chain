package stest.apollo.wallet.dailybuild.account;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AccountResourceMessage;
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
public class WalletTestAccount012 {
  private static final long sendAmount = 10000000000L;
  private static final long frozenAmountForTronPower = 3456789L;
  private static final long frozenAmountForNet = 7000000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenAddress = ecKey1.getAddress();
  String frozenKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(frozenKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true, description = "Freeze balance to get apollo power")
  public void test01FreezeBalanceGetTronPower() {


    final Long beforeFrozenTime = System.currentTimeMillis();
    Assert.assertTrue(PublicMethed.sendcoin(frozenAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeTotalTronPowerWeight = accountResource.getTotalApolloPowerWeight();
    final Long beforeTronPowerLimit = accountResource.getApolloPowerLimit();


    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,frozenAmountForNet,
        0,0,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long afterFrozenTime = System.currentTimeMillis();
    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getApolloPower().getFrozenBalance(),frozenAmountForTronPower);
    Assert.assertTrue(account.getApolloPower().getExpireTime() > beforeFrozenTime
        && account.getApolloPower().getExpireTime() < afterFrozenTime);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalTronPowerWeight = accountResource.getTotalApolloPowerWeight();
    Long afterTronPowerLimit = accountResource.getApolloPowerLimit();
    Long afterTronPowerUsed = accountResource.getApolloPowerUsed();
    Assert.assertEquals(afterTotalTronPowerWeight - beforeTotalTronPowerWeight,
        frozenAmountForTronPower / 1000000L);

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        frozenAmountForTronPower / 1000000L);



    Assert.assertTrue(PublicMethed.freezeBalanceGetTronPower(frozenAddress,
        6000000 - frozenAmountForTronPower,
        0,2,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerLimit = accountResource.getApolloPowerLimit();

    Assert.assertEquals(afterTronPowerLimit - beforeTronPowerLimit,
        6);


  }


  @Test(enabled = true,description = "Vote witness by apollo power")
  public void test02VotePowerOnlyComeFromTronPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeTronPowerUsed = accountResource.getApolloPowerUsed();


    HashMap<byte[],Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress,frozenAmountForNet / 1000000L);
    Assert.assertFalse(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    witnessMap.put(witnessAddress,frozenAmountForTronPower / 1000000L);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTronPowerUsed = accountResource.getApolloPowerUsed();
    Assert.assertEquals(afterTronPowerUsed - beforeTronPowerUsed,
        frozenAmountForTronPower / 1000000L);

    final Long secondBeforeTronPowerUsed = afterTronPowerUsed;
    witnessMap.put(witnessAddress,(frozenAmountForTronPower / 1000000L) - 1);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterTronPowerUsed = accountResource.getApolloPowerUsed();
    Assert.assertEquals(secondBeforeTronPowerUsed - afterTronPowerUsed,
        1);


  }

  @Test(enabled = true,description = "apollo power is not allow to others")
  public void test03TronPowerIsNotAllowToOthers() {
    Assert.assertFalse(PublicMethed.freezeBalanceGetTronPower(frozenAddress,
        frozenAmountForTronPower, 0,2,
        ByteString.copyFrom(foundationAddress),frozenKey,blockingStubFull));
  }


  @Test(enabled = true,description = "Unfreeze balance for apollo power")
  public void test04UnfreezeBalanceForTronPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(foundationAddress, blockingStubFull);
    final Long beforeTotalTronPowerWeight = accountResource.getTotalApolloPowerWeight();


    Assert.assertTrue(PublicMethed.unFreezeBalance(frozenAddress,frozenKey,2,
        null,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalTronPowerWeight = accountResource.getTotalApolloPowerWeight();
    Assert.assertEquals(beforeTotalTronPowerWeight - afterTotalTronPowerWeight,
        6);

    Assert.assertEquals(accountResource.getApolloPowerLimit(),0L);
    Assert.assertEquals(accountResource.getApolloPowerUsed(),0L);

    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getApolloPower().getFrozenBalance(),0);


  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 2, null,
        blockingStubFull);
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 0, null,
        blockingStubFull);
    PublicMethed.freedResource(frozenAddress, frozenKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


