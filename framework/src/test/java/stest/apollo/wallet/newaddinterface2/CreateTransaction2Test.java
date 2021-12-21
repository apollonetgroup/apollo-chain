package stest.apollo.wallet.newaddinterface2;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.api.GrpcAPI.Return;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.contract.BalanceContract.FreezeBalanceContract;
import org.apollo.protos.contract.BalanceContract.TransferContract;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class CreateTransaction2Test {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");

  /*  //testng001、testng002、testng003、testng004
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");
  private static final byte[] toAddress = Base58
      .decodeFromBase58Check("TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU");*/
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  //receipt account
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiptAccountAddress = ecKey2.getAddress();
  String receiptAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendAccountAddress = ecKey1.getAddress();
  String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

  }

  @Test
  public void testSendCoin2() {
    Assert.assertTrue(PublicMethed.sendcoin(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull));

    logger.info(receiptAccountKey);
    Account sendAccount = PublicMethed.queryAccount(sendAccountKey, blockingStubFull);
    Long sendAccountBeforeBalance = sendAccount.getBalance();
    Assert.assertTrue(sendAccountBeforeBalance == 90000000000L);
    Account receiptAccount = PublicMethed.queryAccount(receiptAccountKey, blockingStubFull);
    Long receiptAccountBeforeBalance = receiptAccount.getBalance();
    Assert.assertTrue(receiptAccountBeforeBalance == 0);
    //normal sendcoin2
    Return ret1 = PublicMethed.sendcoin2(receiptAccountAddress, 49880000000L,
        sendAccountAddress, sendAccountKey, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");

    sendAccount = PublicMethed.queryAccount(sendAccountKey, blockingStubFull);
    Long sendAccountAfterBalance = sendAccount.getBalance();
    logger.info(Long.toString(sendAccountAfterBalance));
    Assert.assertTrue(sendAccountAfterBalance == 90000000000L - 49880000000L - 100000L);

    receiptAccount = PublicMethed.queryAccount(receiptAccountKey, blockingStubFull);
    Long receiptAccountAfterBalance = receiptAccount.getBalance();
    Assert.assertTrue(receiptAccountAfterBalance == 49880000000L);
    //Send coin failed due to no enough balance.
    ret1 = PublicMethed
        .sendcoin2(receiptAccountAddress, 9199999999999999999L, sendAccountAddress, sendAccountKey,
            blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Validate TransferContract error, balance is not sufficient.");
    //Send coin failed due to the amount is 0.
    ret1 = PublicMethed
        .sendcoin2(receiptAccountAddress, 0L, sendAccountAddress, sendAccountKey, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Amount must greater than 0.");
    //Send coin failed due to the amount is -1Trx.
    ret1 = PublicMethed
        .sendcoin2(receiptAccountAddress, -1000000L, sendAccountAddress, sendAccountKey,
            blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Amount must greater than 0.");

    //Send coin to yourself
    ret1 = PublicMethed.sendcoin2(sendAccountAddress, 1000000L, sendAccountAddress, sendAccountKey,
        blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.CONTRACT_VALIDATE_ERROR);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(),
        "contract validate error : Cannot transfer trx to yourself.");
    //transfer all balance
    ret1 = PublicMethed.sendcoin2(receiptAccountAddress, 40119900000L,
        sendAccountAddress, sendAccountKey, blockingStubFull);
    Assert.assertEquals(ret1.getCode(), Return.response_code.SUCCESS);
    Assert.assertEquals(ret1.getMessage().toStringUtf8(), "");

    sendAccount = PublicMethed.queryAccount(sendAccountKey, blockingStubFull);
    Long sendAccountAfterBalance1 = sendAccount.getBalance();
    logger.info(Long.toString(sendAccountAfterBalance1));
    Assert.assertTrue(
        sendAccountAfterBalance1 == 90000000000L - 49880000000L - 100000 - 40119900000L);

    receiptAccount = PublicMethed.queryAccount(receiptAccountKey, blockingStubFull);
    Long receiptAccountAfterBalance1 = receiptAccount.getBalance();
    logger.info(Long.toString(receiptAccountAfterBalance1));
    Assert.assertTrue(receiptAccountAfterBalance1 == 49880000000L + 40119900000L);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey) {
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
    ECKey ecKey = temKey;
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Account beforeFronzen = queryAccount(ecKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    //Long beforeBandwidth     = beforeFronzen.getBandwidth();
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      //beforeBandwidth     = beforeFronzen.getBandwidth();
      //logger.info(Long.toString(beforeFronzen.getBandwidth()));
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }

    Long afterBlockNum = 0L;
    Integer wait = 0;
    while (afterBlockNum < beforeBlockNum + 1 && wait < 10) {
      Block currentBlock1 = searchBlockingStubFull
          .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
      wait++;
      try {
        Thread.sleep(2000);
        logger.info("wait 2 second");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Account afterFronzen = queryAccount(ecKey, searchBlockingStubFull);
    Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
    //Long afterBandwidth     = afterFronzen.getBandwidth();
    //logger.info(Long.toString(afterFronzen.getBandwidth()));
    logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
    //logger.info(Integer.toString(search.getFrozenCount()));
    logger.info(
        "beforefronen" + beforeFrozenBalance.toString() + "    afterfronzen" + afterFrozenBalance
            .toString());
    Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
    //Assert.assertTrue(afterBandwidth - beforeBandwidth == freezeBalance * frozen_duration);
    return true;


  }

  /**
   * constructor.
   */

  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = queryAccount(ecKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
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

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

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


