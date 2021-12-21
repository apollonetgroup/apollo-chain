package org.apollo.core.db;

import com.google.protobuf.ByteString;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.FileUtil;
import org.apollo.common.utils.Pair;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.core.Constant;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.KhaosDatabase;
import org.apollo.core.exception.BadNumberBlockException;
import org.apollo.core.exception.NonCommonBlockException;
import org.apollo.core.exception.UnLinkedBlockException;
import org.apollo.protos.Protocol.Block;
import org.apollo.protos.Protocol.BlockHeader;
import org.apollo.protos.Protocol.BlockHeader.raw;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.collections.Lists;

@Slf4j
public class KhaosDatabaseTest {

  private static final String dbPath = "output-khaosDatabase-test";
  private static KhaosDatabase khaosDatabase;
  private static ApolloApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    khaosDatabase = context.getBean(KhaosDatabase.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testStartBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
    khaosDatabase.start(blockCapsule);

    Assert.assertEquals(blockCapsule, khaosDatabase.getBlock(blockCapsule.getBlockId()));
  }

  @Test
  public void testPushGetBlock() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "9938a342238077182498b464ac029222ae169360e540d1fd6aee7c2ae9575a06"))))).build());
    khaosDatabase.start(blockCapsule);
    try {
      khaosDatabase.push(blockCapsule2);
    } catch (UnLinkedBlockException | BadNumberBlockException e) {
      System.out.println(e.getMessage());
    }

    Assert.assertEquals(blockCapsule2, khaosDatabase.getBlock(blockCapsule2.getBlockId()));
    Assert.assertTrue("contain is error", khaosDatabase.containBlock(blockCapsule2.getBlockId()));

    khaosDatabase.removeBlk(blockCapsule2.getBlockId());

    Assert.assertNull("removeBlk is error", khaosDatabase.getBlock(blockCapsule2.getBlockId()));
  }


  @Test
  public void checkWeakReference() throws UnLinkedBlockException, BadNumberBlockException {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82")))
            .setNumber(0))).build());
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder()
            .setParentHash(ByteString.copyFrom(blockCapsule.getBlockId().getBytes())).setNumber(1)))
        .build());
    Assert.assertEquals(blockCapsule.getBlockId(), blockCapsule2.getParentHash());

    khaosDatabase.start(blockCapsule);
    khaosDatabase.push(blockCapsule2);

    khaosDatabase.removeBlk(blockCapsule.getBlockId());
    logger.info("*** " + khaosDatabase.getBlock(blockCapsule.getBlockId()));
    Object object = new Object();
    Reference<Object> objectReference = new WeakReference<>(object);
    blockCapsule = null;
    object = null;
    System.gc();
    logger.info("***** object ref:" + objectReference.get());
    Assert.assertNull(objectReference.get());
    Assert.assertNull(khaosDatabase.getParentBlock(blockCapsule2.getBlockId()));
  }

  @Test
  public void testGetBranch() {
    final String mockedHash = "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b82";
    // common parent block
    BlockCapsule parentBlock = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(mockedHash)))
            .setNumber(0))).build());
    // fork-chain-A
    // longer than chainB, share the common parent block with fork-chain-B
    BlockCapsule block1OnforkA = new BlockCapsule(
        1, parentBlock.getBlockId(), 0, ByteString.EMPTY);
    BlockCapsule block2OnforkA = new BlockCapsule(
        2, block1OnforkA.getBlockId(), 0, ByteString.EMPTY);
    List<KhaosDatabase.KhaosBlock> forkA = Lists.newLinkedList();
    forkA.add(new KhaosDatabase.KhaosBlock(block2OnforkA));
    forkA.add(new KhaosDatabase.KhaosBlock(block1OnforkA));
    forkA.add(new KhaosDatabase.KhaosBlock(parentBlock));
    // fork-chain-B
    BlockCapsule block1OnforkB = new BlockCapsule(
        1, parentBlock.getBlockId(), 0, ByteString.EMPTY);
    List<KhaosDatabase.KhaosBlock> forkB = Lists.newLinkedList();
    forkA.add(new KhaosDatabase.KhaosBlock(block1OnforkB));
    forkA.add(new KhaosDatabase.KhaosBlock(parentBlock));

    khaosDatabase.start(parentBlock);
    try {
      khaosDatabase.push(block1OnforkA);
      khaosDatabase.push(block2OnforkA);
      khaosDatabase.push(block1OnforkB);
      // case: block num of param1 > block num of param2
      Pair result1 = khaosDatabase.getBranch(
          Sha256Hash.of(
              CommonParameter
                  .getInstance().isECKeyCryptoEngine(),
              block2OnforkA.getInstance().getBlockHeader().getRawData().toByteArray()),
          Sha256Hash.of(
              CommonParameter
                  .getInstance().isECKeyCryptoEngine(),
              block1OnforkB.getInstance().getBlockHeader().getRawData().toByteArray()));
      Assert.assertEquals(forkA, result1.getKey());
      Assert.assertEquals(forkB, result1.getValue());
      // case: block num of param2 > block num of param1
      Pair result2 = khaosDatabase.getBranch(
          Sha256Hash.of(
              CommonParameter
                  .getInstance().isECKeyCryptoEngine(),
              block1OnforkB.getInstance().getBlockHeader().getRawData().toByteArray()),
          Sha256Hash.of(
              CommonParameter
                  .getInstance().isECKeyCryptoEngine(),
              block2OnforkA.getInstance().getBlockHeader().getRawData().toByteArray()));
      Assert.assertEquals(forkB, result2.getKey());
      Assert.assertEquals(forkA, result2.getValue());
    } catch (UnLinkedBlockException | BadNumberBlockException | NonCommonBlockException e) {
      System.out.println(e.getMessage());
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testIsNotEmpty() {
    BlockCapsule blockCapsule = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))))).build());
    khaosDatabase.start(blockCapsule);
    khaosDatabase.isNotEmpty();
  }
}