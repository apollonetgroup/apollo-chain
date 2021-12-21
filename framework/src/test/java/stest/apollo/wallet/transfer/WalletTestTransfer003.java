package stest.apollo.wallet.transfer;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletExtensionGrpc;
import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.BytesMessage;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.Protocol.TransactionInfo;
import org.apollo.protos.contract.BalanceContract;
import org.apollo.protos.contract.AccountContract.AccountUpdateContract;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.Sha256Hash;
import stest.apollo.wallet.common.client.utils.TransactionUtils;


@Slf4j
public class WalletTestTransfer003 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "transaction007_" + Long.toString(now);
  private static Protocol.Transaction sendCoinTransaction;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final Long createUseFee = 100000L;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendCoinAddress = ecKey1.getAddress();
  String testKeyForSendCoin = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey2.getAddress();
  String testKeyForNewAccount = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /*  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }*/

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  private static Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * constructor.
   */

  public static Protocol.Transaction sendcoin(byte[] to, long amount, byte[] owner, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(priKey, blockingStubFull);

    BalanceContract.TransferContract.Builder builder = BalanceContract.TransferContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    BalanceContract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (!response.getResult()) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    }
    return transaction;
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    logger.info(testKeyForSendCoin);
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
    blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);

  }

  @Test(enabled = true)
  public void test1UseFeeOrNet() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    sendCoinAddress = ecKey1.getAddress();
    testKeyForSendCoin = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    newAccountAddress = ecKey2.getAddress();
    testKeyForNewAccount = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(sendCoinAddress, 200000L,
        fromAddress, testKey002, blockingStubFull));
    Long feeNum = 0L;
    Long netNum = 0L;
    Long sendNum = 0L;
    Long feeCost = 0L;
    Long times = 0L;
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long beforeBalance = sendAccountInfo.getBalance();
    Long netUsed1 = 0L;
    Long netUsed2 = 1L;
    logger.info("Before test, the account balance is " + Long.toString(beforeBalance));

    while (!(netUsed1.equals(netUsed2))) {
      sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
      netUsed1 = sendAccountInfo.getFreeNetUsage();
      sendCoinTransaction = sendcoin(fromAddress, 1L, sendCoinAddress,
          testKeyForSendCoin, blockingStubFull);

      sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
      netUsed2 = sendAccountInfo.getFreeNetUsage();

      if (times++ < 1) {
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        //PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
        String txId = ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
            .isECKeyCryptoEngine(), sendCoinTransaction
            .getRawData().toByteArray()));
        logger.info(txId);
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
        Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);
        logger.info("solidity block num is " + Long.toString(getTransactionById
            .get().getBlockNumber()));
        Assert.assertTrue(getTransactionById.get().getBlockNumber() > 0);
      }

      logger.info(Long.toString(netUsed1));
      logger.info(Long.toString(netUsed2));
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Assert.assertTrue(netUsed2 > 4500);
    //Next time, use fee
    sendCoinTransaction = sendcoin(fromAddress, 1L, sendCoinAddress,
        testKeyForSendCoin, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
    String txId = ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), sendCoinTransaction
        .getRawData().toByteArray()));
    logger.info(txId);
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);
    logger.info(getTransactionById.get().toString());
    logger.info("when use fee, the block num is " + Long.toString(getTransactionById
        .get().getBlockNumber()));
    Assert.assertTrue(getTransactionById.get().getFee() > 0);
    Assert.assertTrue(getTransactionById.get().getBlockNumber() > 0);
  }

  @Test(enabled = true)
  public void test2CreateAccountUseFee() {
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long beforeBalance = sendAccountInfo.getBalance();
    logger.info("before balance " + Long.toString(beforeBalance));
    Long times = 0L;
    sendCoinTransaction = sendcoin(newAccountAddress, 1L, sendCoinAddress,
        testKeyForSendCoin, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
    String txId = ByteArray.toHexString(Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), sendCoinTransaction
        .getRawData().toByteArray()));
    logger.info(txId);
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Optional<TransactionInfo> getTransactionById = Optional.ofNullable(transactionInfo);

    logger.info("In create account case, the fee is " + getTransactionById.get().getFee());
    Assert.assertTrue(getTransactionById.get().getFee() == createUseFee);

    sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    final Long afterBalance = sendAccountInfo.getBalance();
    logger.info("after balance " + Long.toString(afterBalance));
    Assert.assertTrue(afterBalance + 1L + createUseFee == beforeBalance);
  }

  @Test(enabled = true)
  public void test3InvalidGetTransactionById() {
    String txId = "";
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);
    Optional<Transaction> getTransactionById = Optional.ofNullable(transaction);
    Assert.assertTrue(getTransactionById.get().getRawData().getContractCount() == 0);

    txId = "1";
    bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    request = BytesMessage.newBuilder().setValue(bsTxid).build();
    transaction = blockingStubFull.getTransactionById(request);
    getTransactionById = Optional.ofNullable(transaction);
    Assert.assertTrue(getTransactionById.get().getRawData().getContractCount() == 0);
  }

  @Test(enabled = true)
  public void test4NoBalanceCanSend() {
    Long feeNum = 0L;
    Account sendAccountInfo = PublicMethed.queryAccount(testKeyForSendCoin, blockingStubFull);
    Long beforeBalance = sendAccountInfo.getBalance();
    logger.info("Before test, the account balance is " + Long.toString(beforeBalance));
    while (feeNum < 250) {
      sendCoinTransaction = sendcoin(fromAddress, 10L, sendCoinAddress,
          testKeyForSendCoin, blockingStubFull);
      feeNum++;
    }
    Assert.assertTrue(PublicMethed.waitProduceNextBlock(blockingStubFull));

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

  /**
   * constructor.
   */

  public Protocol.Transaction updateAccount(byte[] addressBytes, byte[] accountNameBytes,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    AccountUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (!response.getResult()) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    }
    return transaction;
  }
}


