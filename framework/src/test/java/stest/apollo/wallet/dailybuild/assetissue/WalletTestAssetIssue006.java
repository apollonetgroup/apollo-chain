package stest.apollo.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.bouncycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAssetIssue006 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "assetissue006" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String description = "test query assetissue by timestamp from soliditynode";
  String url = "https://testqueryassetissue.com/bytimestamp/from/soliditynode/";
  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] queryAssetIssueFromSoliAddress = ecKey.getAddress();
  String queryAssetIssueKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /*  @Test(enabled = true)
  public void testGetAssetIssueListByTimestamp() {
      Assert.assertTrue(PublicMethed.freezeBalance(fromAddress,10000000,3,testKey002,
        blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(queryAssetIssueFromSoliAddress,2048000000,fromAddress,
        testKey002,blockingStubFull));
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(queryAssetIssueFromSoliAddress, name,
        totalSupply, 1, 100, start, end, 1, description, url, 1000L,
        1000L,1L,1L,queryAssetIssueKey,blockingStubFull));
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Block solidityCurrentBlock = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage
        .newBuilder().build());
    Integer wait = 0;
    while (solidityCurrentBlock.getBlockHeader().getRawData().getNumber()
        < currentBlock.getBlockHeader().getRawData().getNumber() + 1 && wait < 10) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.info("Solidity didn't synchronize the fullnode block,please wait");
      solidityCurrentBlock = blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder()
          .build());
      wait++;
      if (wait == 9) {
        logger.info("Didn't syn,skip to next case.");
      }
    }


    long time = now;
    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    GrpcAPI.AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueListByTimestamp(timeStamp.build());
    Optional<GrpcAPI.AssetIssueList> getAssetIssueListByTimestamp = Optional
        .ofNullable(assetIssueList);

    Assert.assertTrue(getAssetIssueListByTimestamp.isPresent());
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() > 0);
    logger.info(Integer.toString(getAssetIssueListByTimestamp.get().getAssetIssueCount()));
    for (Integer j = 0; j < getAssetIssueListByTimestamp.get().getAssetIssueCount(); j++) {
      Assert.assertFalse(getAssetIssueListByTimestamp.get().getAssetIssue(j).getName().isEmpty());
      Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssue(j).getTotalSupply() > 0);
      Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssue(j).getNum() > 0);
      logger.info(
          Long.toString(getAssetIssueListByTimestamp.get().getAssetIssue(j).getTotalSupply()));
    }

  }

  @Test(enabled = true)
  public void testExceptionGetAssetIssueListByTimestamp() {
    //Time stamp is below zero.
    long time = -1000000000;
    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    GrpcAPI.AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueListByTimestamp(timeStamp.build());
    Optional<GrpcAPI.AssetIssueList> getAssetIssueListByTimestamp = Optional
        .ofNullable(assetIssueList);
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() == 0);

    //No asset issue was create
    time = 1000000000;
    timeStamp = NumberMessage.newBuilder();
    timeStamp.setNum(time);
    assetIssueList = blockingStubSolidity.getAssetIssueListByTimestamp(timeStamp.build());
    getAssetIssueListByTimestamp = Optional.ofNullable(assetIssueList);
    Assert.assertTrue(getAssetIssueListByTimestamp.get().getAssetIssueCount() == 0);

  }*/

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);


  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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
}


