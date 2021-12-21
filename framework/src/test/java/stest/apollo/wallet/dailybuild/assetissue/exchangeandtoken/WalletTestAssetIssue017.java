package stest.apollo.wallet.dailybuild.assetissue.exchangeandtoken;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.AssetIssueList;
import org.apollo.api.GrpcAPI.PaginatedMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.core.db.Manager;
import org.apollo.protos.Protocol;
import org.apollo.protos.contract.AssetIssueContractOuterClass;
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
public class WalletTestAssetIssue017 {

  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static long start;
  private static long end;
  private static long now = System.currentTimeMillis();
  private static String name = "AssetIssue017_" + Long.toString(now);
  private static long totalSupply = now;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue017";
  String url = "https://stest.assetissue016.url";
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset017Address = ecKey1.getAddress();
  String testKeyForAssetIssue017 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Manager dbManager;
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  public static Boolean createAssetIssue(byte[] address, String name, Long totalSupply,
      Integer trxNum, Integer icoNum, Long startTime, Long endTime, Integer voteScore,
      String description, String url, Long freeAssetNetLimit, Long publicFreeAssetNetLimit,
      Long fronzenAmount, Long frozenDay, String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    //Protocol.Account search = queryAccount(ecKey, blockingStubFull);
    try {
      AssetIssueContractOuterClass.AssetIssueContract.Builder builder =
          AssetIssueContractOuterClass.AssetIssueContract
              .newBuilder();
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
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContractOuterClass.AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
      if (response.getResult() == false) {
        logger.info("failed reason is " + ByteArray.toStr(response.getMessage().toByteArray()));
        return false;
      } else {
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

  public static Protocol.Transaction signTransaction(ECKey ecKey,
      Protocol.Transaction transaction) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    if (ecKey == null || ecKey.getPrivKey() == null) {
      //logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
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
    logger.info(testKeyForAssetIssue017);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void atestGetPaginatedAssetIssueList() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset017Address = ecKey1.getAddress();
    testKeyForAssetIssue017 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed
        .sendcoin(asset017Address, sendAmount, fromAddress, testKey002, blockingStubFull));
    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
    now = System.currentTimeMillis();
    name = "AssetIssue017_" + Long.toString(now);
    totalSupply = now;
    Assert.assertTrue(createAssetIssue(asset017Address, name, totalSupply, 1, 1,
        start, end, 1, description, url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L,
        1L, testKeyForAssetIssue017, blockingStubFull));

    Integer offset = 0;
    Integer limit = 100;

    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);

    AssetIssueList assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
    for (Integer i = 0; i < assetIssueListPaginated.get().getAssetIssueCount(); i++) {
      Assert.assertTrue(assetIssueListPaginated.get().getAssetIssue(i).getTotalSupply() > 0);
    }
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
  }

  @Test(enabled = true)
  public void btestGetPaginatedAssetIssueListException() {
    //offset is 0, limit is 0.
    Integer offset = 0;
    Integer limit = 0;
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is -1, limit is 100.
    offset = -1;
    limit = 100;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is -1.
    offset = 0;
    limit = -1;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is 50.
    offset = 0;
    limit = 50;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubFull
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
  }

  @Test(enabled = true)
  public void ctestGetPaginatedAssetIssueListOnSolidityNode() {

    Integer offset = 0;
    Integer limit = 100;

    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    Assert.assertTrue(PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,
        blockingStubSolidity));
    AssetIssueList assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);

    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);
    for (Integer i = 0; i < assetIssueListPaginated.get().getAssetIssueCount(); i++) {
      Assert.assertTrue(assetIssueListPaginated.get().getAssetIssue(i).getTotalSupply() > 0);
    }
  }

  @Test(enabled = true)
  public void dtestGetPaginatedAssetIssueListExceptionOnSolidityNode() {
    //offset is 0, limit is 0.
    Integer offset = 0;
    Integer limit = 0;
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    Optional<AssetIssueList> assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is -1.
    offset = 0;
    limit = -1;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);

    //offset is 0, limit is 50.
    offset = 0;
    limit = 50;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);

    //offset is 0, limit is 1000.
    offset = 0;
    limit = 1000;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() >= 1);

    //offset is -1, limit is 100.
    offset = -1;
    limit = 100;
    pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    assetIssueList = blockingStubSolidity
        .getPaginatedAssetIssueList(pageMessageBuilder.build());
    assetIssueListPaginated = Optional.ofNullable(assetIssueList);
    logger.info(Long.toString(assetIssueListPaginated.get().getAssetIssueCount()));
    Assert.assertTrue(assetIssueListPaginated.get().getAssetIssueCount() == 0);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}