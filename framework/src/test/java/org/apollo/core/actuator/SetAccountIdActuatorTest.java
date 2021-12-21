package org.apollo.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.actuator.SetAccountIdActuator;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.TransactionResultCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;
import org.apollo.protos.Protocol.AccountType;
import org.apollo.protos.Protocol.Transaction.Result.code;
import org.apollo.protos.contract.AssetIssueContractOuterClass;
import org.apollo.protos.contract.AccountContract.SetAccountIdContract;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class SetAccountIdActuatorTest {

  private static final String dbPath = "output_setaccountid_test";
  private static final String ACCOUNT_NAME = "ownertest";
  private static final String ACCOUNT_NAME_1 = "ownertest1";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_1;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static ApolloApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_1 = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
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
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.EMPTY,
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_1));
    dbManager.getAccountIdIndexStore().delete(ACCOUNT_NAME.getBytes());
    dbManager.getAccountIdIndexStore().delete(ACCOUNT_NAME_1.getBytes());
  }

  private Any getContract(String name, String address) {
    return Any.pack(
        SetAccountIdContract.newBuilder()
            .setAccountId(ByteString.copyFromUtf8(name))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  /**
   * Unit test.
   */

  private Any getContract(ByteString name, String address) {
    return Any.pack(
        SetAccountIdContract.newBuilder()
            .setAccountId(name)
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .build());
  }

  /**
   * set account id when all right.
   */
  @Test
  public void rightSetAccountId() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    SetAccountIdActuator actuator = new SetAccountIdActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountId().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidAddress() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    SetAccountIdActuator actuator = new SetAccountIdActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_INVALID));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noExistAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    SetAccountIdActuator actuator = new SetAccountIdActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_1));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has not existed", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void twiceUpdateAccount() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    SetAccountIdActuator actuator = new SetAccountIdActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS));
    SetAccountIdActuator actuator1 = new SetAccountIdActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME_1, OWNER_ADDRESS));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountId().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This account id already set", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountId().toStringUtf8());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void nameAlreadyUsed() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    SetAccountIdActuator actuator = new SetAccountIdActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS));
    SetAccountIdActuator actuator1 = new SetAccountIdActuator();
    actuator1.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(ACCOUNT_NAME, OWNER_ADDRESS_1));
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountId().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_1)),
            ByteString.EMPTY,
            AccountType.Normal);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    try {
      actuator1.validate();
      actuator1.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("This id has existed", e.getMessage());
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(ACCOUNT_NAME, accountCapsule.getAccountId().toStringUtf8());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  /*
   * Account name need 8 - 32 bytes.
   */
  public void invalidName() {
    TransactionResultCapsule ret = new TransactionResultCapsule();
    //Just OK 32 bytes is OK
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testname0123456789abcdefghijgklm", OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals("testname0123456789abcdefghijgklm",
          accountCapsule.getAccountId().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //8 bytes is OK
    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("test1111", OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      accountCapsule = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals("test1111",
          accountCapsule.getAccountId().toStringUtf8());
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Empty name
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract(ByteString.EMPTY, OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountId", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Too long name 33 bytes
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testname0123456789abcdefghijgklmo0123456789abcdefghijgk"
              + "lmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
              + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo0123456789abcdefghijgklmo"
              + "0123456789abcdefghijgklmo0123456789abcdefghijgklmo", OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountId", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Too short name 7 bytes
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("testnam", OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountId", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Can't contain space
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager())
          .setAny(getContract("t e", OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountId", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //Can't contain chinese characters
    accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setAccountId(ByteString.EMPTY.toByteArray());
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      SetAccountIdActuator actuator = new SetAccountIdActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
          getContract(ByteString.copyFrom(ByteArray.fromHexString("E6B58BE8AF95")), OWNER_ADDRESS));
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid accountId", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void commonErrorCheck() {

    SetAccountIdActuator actuator = new SetAccountIdActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [SetAccountIdContract],real type[");
    actuatorTest.invalidContractType();

    actuatorTest.setContract(getContract(ACCOUNT_NAME, OWNER_ADDRESS));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or account id index store!");
    actuatorTest.nullDBManger();

  }


}
