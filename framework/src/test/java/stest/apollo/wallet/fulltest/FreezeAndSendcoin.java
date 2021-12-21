package stest.apollo.wallet.fulltest;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.api.GrpcAPI.Return;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.contract.BalanceContract;
import org.apollo.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class FreezeAndSendcoin {

  private static final long now = System.currentTimeMillis();
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final String testKey003 =
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final Long sendAmount = 10000000L;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeAddress = ecKey1.getAddress();
  String testKeyForFreeze = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */

  public static Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI
        .EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long beforeFrozenBalance = 0L;
    //Long beforeBandwidth     = beforeFronzen.getBandwidth();

    BalanceContract.FreezeBalanceContract.Builder builder = BalanceContract.FreezeBalanceContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    BalanceContract.FreezeBalanceContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }

    Long afterBlockNum = 0L;

    while (afterBlockNum < beforeBlockNum) {
      Protocol.Block currentBlock1 = blockingStubFull.getNowBlock(GrpcAPI
          .EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
    }
    return true;
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  //@Test(enabled = false)

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    /*    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4);
    try {
      randNum = rand.nextInt(20000);
      Thread.sleep(randNum);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/

    logger.info(testKeyForFreeze);
    logger.info(transferAssetCreateKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, threadPoolSize = 500, invocationCount = 1000)
  public void freezeAndSendcoin() throws InterruptedException {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] freezeAddress = ecKey1.getAddress();
    String testKeyForFreeze = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Account toAccountInfo = PublicMethed.queryAccount(testKey003, blockingStubFull);
    Account freezeAccountInfo = PublicMethed.queryAccount(testKeyForFreeze, blockingStubFull);

    Integer i = 0;
    Boolean ret = false;
    Boolean sendRet = false;
    Boolean updateRet = false;
    Boolean participateRet = false;
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;

    while (toAccountInfo.getBalance() > 10000009L) {
      randNum = rand.nextInt(3);
      ManagedChannel channelFull = null;
      WalletGrpc.WalletBlockingStub blockingStubFull = null;
      fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
          .get(randNum);
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

      freezeBalance(freezeAddress, 3000000L, 3L, testKeyForFreeze, blockingStubFull);
      PublicMethed
          .sendcoin(freezeAddress, sendAmount, toAddress, testKey003, blockingStubFull);

      ret = freezeBalance(freezeAddress, 1000000L, 3L, testKeyForFreeze, blockingStubFull);
      freezeBalance(freezeAddress, 1000000L, 3L, testKeyForFreeze, blockingStubFull);
      freezeBalance(freezeAddress, 1000000L, 3L, testKeyForFreeze, blockingStubFull);

      if (ret) {
        logger.info("New account freeze success " + Integer.toString(i));
        sendRet = PublicMethed.sendcoin(toAddress, 6000000L, freezeAddress,
            testKeyForFreeze, blockingStubFull);
        if (sendRet) {
          logger.info("This account transfer coin back. " + Integer.toString(i));
          freezeAccountInfo = PublicMethed.queryAccount(testKeyForFreeze, blockingStubFull);
          logger.info("This account now has balance is " + Long
              .toString(freezeAccountInfo.getBalance()));

        }

      }

      unFreezeBalance(freezeAddress, testKeyForFreeze);
      withdrawBalance(freezeAddress, testKeyForFreeze);

      ecKey1 = new ECKey(Utils.getRandom());
      freezeAddress = ecKey1.getAddress();
      testKeyForFreeze = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      toAccountInfo = PublicMethed.queryAccount(testKey003, blockingStubFull);
      logger.info("Now the toaddress balance is " + Long.toString(toAccountInfo.getBalance()));
      NumberMessage beforeGetTotalTransaction = blockingStubFull
          .totalTransaction(GrpcAPI.EmptyMessage.newBuilder().build());
      logger.info("Now total transaction is " + Long.toString(beforeGetTotalTransaction.getNum()));
      ret = false;
      sendRet = false;
      i++;

      /*      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        try {
          //randNum = rand.nextInt(10000) + 3000;
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

      }*/

    }
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public boolean unFreezeBalance(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Account search = queryAccount(ecKey, blockingStubFull);

    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress);

    UnfreezeBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public boolean withdrawBalance(byte[] address, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    BalanceContract.WithdrawBalanceContract.Builder builder =
        BalanceContract.WithdrawBalanceContract
            .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress);
    BalanceContract.WithdrawBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.withdrawBalance(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    }
    logger.info("test withdraw" + priKey);
    return true;

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

}


