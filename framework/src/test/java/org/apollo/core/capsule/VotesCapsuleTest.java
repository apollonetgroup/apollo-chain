package org.apollo.core.capsule;

import com.google.protobuf.ByteString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.utils.FileUtil;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.capsule.VotesCapsule;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.TransactionStoreTest;
import org.apollo.protos.Protocol.Vote;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;


@Slf4j
public class VotesCapsuleTest {

  private static String dbPath = "output_votesCapsule_test";
  private static final String OWNER_ADDRESS;
  private static List<Vote> oldVotes;

  static {
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    oldVotes = new ArrayList<Vote>();
  }

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);

  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void votesCapsuleTest() {
    ByteString address = StringUtil.hexString2ByteString(OWNER_ADDRESS);
    VotesCapsule votesCapsule = new VotesCapsule(address, oldVotes);

    votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 10);
    votesCapsule.addOldVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 5);
    Assert.assertEquals(votesCapsule.getOldVotes().size(), 2);

    votesCapsule.addNewVotes(ByteString.copyFrom(TransactionStoreTest.randomBytes(32)), 6);
    Assert.assertEquals(votesCapsule.getNewVotes().size(), 1);

    votesCapsule.clearNewVotes();
    Assert.assertTrue(votesCapsule.getNewVotes().isEmpty());

    votesCapsule.clearOldVotes();
    Assert.assertTrue(votesCapsule.getOldVotes().isEmpty());

  }
}