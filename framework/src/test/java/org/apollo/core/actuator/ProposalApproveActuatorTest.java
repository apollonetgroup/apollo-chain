package org.apollo.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.File;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.FileUtil;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.actuator.ProposalApproveActuator;
import org.apollo.core.actuator.ProposalCreateActuator;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.ProposalCapsule;
import org.apollo.core.capsule.TransactionResultCapsule;
import org.apollo.core.capsule.WitnessCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;
import org.apollo.core.exception.ItemNotFoundException;
import org.apollo.protos.Protocol.AccountType;
import org.apollo.protos.Protocol.Proposal.State;
import org.apollo.protos.Protocol.Transaction.Result.code;
import org.apollo.protos.contract.AssetIssueContractOuterClass;
import org.apollo.protos.contract.ProposalContract;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j

public class ProposalApproveActuatorTest {

  private static final String dbPath = "output_ProposalApprove_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static ApolloApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void initTest() {
    WitnessCapsule ownerWitnessFirstCapsule =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            10_000_000L,
            URL);
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getWitnessStore().put(ownerWitnessFirstCapsule.getAddress().toByteArray(),
        ownerWitnessFirstCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(0);

    long id = 1;
    dbManager.getProposalStore().delete(ByteArray.fromLong(1));
    dbManager.getProposalStore().delete(ByteArray.fromLong(2));
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 6 * 27 * 1000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getChainBaseManager().getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      ProposalCapsule proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 1);
      Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
      Assert.assertEquals(proposalCapsule.getCreateTime(), 1000000);
      Assert.assertEquals(proposalCapsule.getExpirationTime(),
          261200000); // 2000000 + 3 * 4 * 21600000
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  private Any getContract(String address, HashMap<Long, Long> paras) {
    return Any.pack(
        ProposalContract.ProposalCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .putAllParameters(paras)
            .build());
  }

  private Any getContract(String address, long id, boolean isAddApproval) {
    return Any.pack(
        ProposalContract.ProposalApproveContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setProposalId(id)
            .setIsAddApproval(isAddApproval)
            .build());
  }

  /**
   * first approveProposal, result is success.
   */
  @Test
  public void successProposalApprove() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      try {
        proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      } catch (ItemNotFoundException e) {
        Assert.assertFalse(e instanceof ItemNotFoundException);
        return;
      }
      Assert.assertEquals(proposalCapsule.getApprovals().size(), 1);
      Assert.assertEquals(proposalCapsule.getApprovals().get(0),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)));
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // isAddApproval == false
    ProposalApproveActuator actuator2 = new ProposalApproveActuator();
    actuator2.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, 1, false));

    TransactionResultCapsule ret2 = new TransactionResultCapsule();
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 1);
    try {
      actuator2.validate();
      actuator2.execute(ret2);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      try {
        proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      } catch (ItemNotFoundException e) {
        Assert.assertFalse(e instanceof ItemNotFoundException);
        return;
      }
      Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_INVALID, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_NOACCOUNT, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use WitnessStore not exists Address,result is failed,exception is "witness not exists".
   */
  @Test
  public void noWitness() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + OWNER_ADDRESS_SECOND + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Proposal not exists, result is failed, exception is "Proposal not exists".
   */
  @Test
  public void noProposal() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 2;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * duplicate approval, result is failed, exception is "Proposal not exists".
   */
  @Test
  public void duplicateApproval() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    proposalCapsule
        .addApproval(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)));
    dbManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    String readableOwnerAddress = StringUtil.createReadableString(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)));
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness [" + readableOwnerAddress + "]has approved proposal[" + id
          + "] before");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + readableOwnerAddress + "]has approved "
              + "proposal[" + id + "] before",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal expired, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalExpired() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(261200100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] expired");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] expired",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal canceled, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalCanceled() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(100100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, true));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      proposalCapsule.setState(State.CANCELED);
      dbManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] canceled");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] canceled",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * if !isAddApproval, and proposal not approved before, result is failed, exception is "Proposal
   * expired".
   */
  @Test
  public void proposalNotApproved() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(100100);
    long id = 1;

    // isAddApproval == true
    ProposalApproveActuator actuator = new ProposalApproveActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id, false));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    String readableOwnerAddress = StringUtil.createReadableString(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)));
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      dbManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness [" + readableOwnerAddress + "]has not approved proposal[" + id + "] before");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + readableOwnerAddress + "]has not approved "
              + "proposal[" + id + "] before",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void commonErrorCheck() {

    ProposalApproveActuator actuator = new ProposalApproveActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [ProposalApproveContract],real type[");
    actuatorTest.invalidContractType();

    actuatorTest.setContract(getContract(OWNER_ADDRESS_FIRST, 1, true));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();

  }


}