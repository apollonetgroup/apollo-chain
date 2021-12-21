package stest.apollo.wallet.dailybuild.assetissue;

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
import org.apollo.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class WalletTestAssetIssue010 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswe"
          + "dcvqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static final String tooLongUrl = "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqas"
      + "wqaswqasw1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazx"
      + "swedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
      + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
  private static String name = "testAssetIssue010_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  String updateDescription = "This is test for update asset issue, case AssetIssue_010";
  String updateUrl = "www.updateassetissue.010.cn";
  Long freeAssetNetLimit = 1000L;
  Long publicFreeAssetNetLimit = 1000L;
  Long updateFreeAssetNetLimit = 10001L;
  Long updatePublicFreeAssetNetLimit = 10001L;
  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] asset010Address = ecKey.getAddress();
  String testKeyForAssetIssue010 = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

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

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Update asset issue")
  public void testUpdateAssetIssue() {
    ecKey = new ECKey(Utils.getRandom());
    asset010Address = ecKey.getAddress();
    testKeyForAssetIssue010 = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(testKeyForAssetIssue010);

    Assert.assertTrue(PublicMethed
        .sendcoin(asset010Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .freezeBalance(asset010Address, 200000000L, 0, testKeyForAssetIssue010,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(asset010Address, name, totalSupply, 1, 1,
        start, end, 1, description, url, freeAssetNetLimit, publicFreeAssetNetLimit,
        1L, 1L, testKeyForAssetIssue010, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed
        .queryAccount(testKeyForAssetIssue010, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    //Query the description and url,freeAssetNetLimit and publicFreeAssetNetLimit
    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder()
        .setValue(assetAccountId).build();
    AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request);

    Assert.assertTrue(
        ByteArray.toStr(assetIssueByName.getDescription().toByteArray()).equals(description));
    Assert.assertTrue(ByteArray.toStr(assetIssueByName.getUrl().toByteArray()).equals(url));
    Assert.assertTrue(assetIssueByName.getFreeAssetNetLimit() == freeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetLimit() == publicFreeAssetNetLimit);

    //Test update asset issue
    Assert.assertTrue(PublicMethed
        .updateAsset(asset010Address, updateDescription.getBytes(), updateUrl.getBytes(),
            updateFreeAssetNetLimit,
            updatePublicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //After update asset issue ,query the description and url,
    // freeAssetNetLimit and publicFreeAssetNetLimit
    assetIssueByName = blockingStubFull.getAssetIssueByName(request);

    Assert.assertTrue(
        ByteArray.toStr(assetIssueByName.getDescription().toByteArray()).equals(updateDescription));
    Assert.assertTrue(ByteArray.toStr(assetIssueByName.getUrl().toByteArray()).equals(updateUrl));
    Assert.assertTrue(assetIssueByName.getFreeAssetNetLimit() == updateFreeAssetNetLimit);
    Assert
        .assertTrue(assetIssueByName.getPublicFreeAssetNetLimit() == updatePublicFreeAssetNetLimit);
  }

  @Test(enabled = true, description = "Update asset issue with exception condition")
  public void testUpdateAssetIssueException() {
    //Test update asset issue for wrong parameter
    //publicFreeAssetNetLimit is -1
    Assert.assertFalse(PublicMethed
        .updateAsset(asset010Address, updateDescription.getBytes(), updateUrl.getBytes(),
            updateFreeAssetNetLimit,
            -1L, testKeyForAssetIssue010, blockingStubFull));
    //publicFreeAssetNetLimit is 0
    Assert.assertTrue(PublicMethed
        .updateAsset(asset010Address, updateDescription.getBytes(), updateUrl.getBytes(),
            updateFreeAssetNetLimit,
            0, testKeyForAssetIssue010, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //FreeAssetNetLimit is -1
    Assert.assertFalse(PublicMethed
        .updateAsset(asset010Address, updateDescription.getBytes(), updateUrl.getBytes(), -1,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    //FreeAssetNetLimit is 0
    Assert.assertTrue(PublicMethed
        .updateAsset(asset010Address, updateDescription.getBytes(), updateUrl.getBytes(), 0,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Description is null
    Assert.assertTrue(PublicMethed
        .updateAsset(asset010Address, "".getBytes(), updateUrl.getBytes(), freeAssetNetLimit,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    //Url is null
    Assert.assertFalse(PublicMethed
        .updateAsset(asset010Address, description.getBytes(), "".getBytes(), freeAssetNetLimit,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    //Too long discription
    Assert.assertFalse(PublicMethed
        .updateAsset(asset010Address, tooLongDescription.getBytes(), url.getBytes(),
            freeAssetNetLimit,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
    //Too long URL
    Assert.assertFalse(PublicMethed
        .updateAsset(asset010Address, description.getBytes(), tooLongUrl.getBytes(),
            freeAssetNetLimit,
            publicFreeAssetNetLimit, testKeyForAssetIssue010, blockingStubFull));
  }

  @AfterMethod
  public void aftertest() {
    PublicMethed
        .freedResource(asset010Address, testKeyForAssetIssue010, fromAddress, blockingStubFull);
    PublicMethed.unFreezeBalance(asset010Address, testKeyForAssetIssue010, 0, asset010Address,
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

  /**
   * constructor.
   */

  public Boolean createAssetIssue(byte[] address, String name, Long totalSupply, Integer trxNum,
      Integer icoNum, Long startTime, Long endTime,
      Integer voteScore, String description, String url, Long fronzenAmount, Long frozenDay,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = PublicMethed.queryAccount(priKey, blockingStubFull);

    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply
              .newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);
      Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
        return false;
      } else {
        logger.info(name);
        return true;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
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

  /**
   * constructor.
   */

  public boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }

  /**
   * constructor.
   */

  public boolean unFreezeAsset(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    UnfreezeAssetContract.Builder builder = UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress);

    UnfreezeAssetContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public boolean participateAssetIssue(byte[] to, byte[] assertName, long amount, byte[] from,
      String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract
        .newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();

    Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      logger.info(name);
      return true;
    }
  }

}


