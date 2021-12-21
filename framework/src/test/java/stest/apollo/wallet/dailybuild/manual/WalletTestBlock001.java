package stest.apollo.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;

@Slf4j
public class WalletTestBlock001 {

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

  /**
   * constructor.
   */
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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from fullnode")
  public void testCurrentBlock() {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock is " + Long.toString(currentBlock.getBlockHeader()
        .getRawData().getNumber()));

    //Improve coverage.
    currentBlock.equals(currentBlock);
    Block newBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    newBlock.equals(currentBlock);
    newBlock.hashCode();
    newBlock.getSerializedSize();
    newBlock.getTransactionsCount();
    newBlock.getTransactionsList();
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Get now block from solidity")
  public void testCurrentBlockFromSolidity() {
    Block currentBlock = blockingStubSolidity
        .getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(currentBlock.hasBlockHeader());
    Assert.assertFalse(currentBlock.getBlockHeader().getWitnessSignature().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getTimestamp() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getWitnessAddress().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getNumber() > 0);
    Assert.assertFalse(currentBlock.getBlockHeader().getRawData().getParentHash().isEmpty());
    Assert.assertTrue(currentBlock.getBlockHeader().getRawData().getWitnessId() >= 0);
    logger.info("test getcurrentblock in soliditynode is " + Long.toString(currentBlock
        .getBlockHeader().getRawData().getNumber()));
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
  }

  /**
   * constructor.
   */

  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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


