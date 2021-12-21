package org.apollo.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.runtime.RuntimeImpl;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.ChainBaseManager;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.capsule.AccountAssetCapsule;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.AssetIssueCapsule;
import org.apollo.core.capsule.TransactionCapsule;
import org.apollo.core.capsule.TransactionResultCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.BandwidthProcessor;
import org.apollo.core.db.Manager;
import org.apollo.core.db.TransactionTrace;
import org.apollo.core.exception.AccountResourceInsufficientException;
import org.apollo.core.exception.ContractValidateException;
import org.apollo.core.exception.TooBigTransactionResultException;
import org.apollo.core.store.StoreFactory;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.AccountType;
import org.apollo.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.apollo.protos.contract.BalanceContract.TransferContract;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class BandwidthProcessorTest {

  private static final String dbPath = "output_bandwidth_test";
  private static final String ASSET_NAME;
  private static final String ASSET_NAME_V2;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_ADDRESS;
  private static final String ASSET_ADDRESS_V2;
  private static final String TO_ADDRESS;
  private static final long TOTAL_SUPPLY = 10000000000000L;
  private static final int TRX_NUM = 2;
  private static final int NUM = 2147483647;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static Manager dbManager;
  private static ChainBaseManager chainBaseManager;
  private static ApolloApplicationContext context;
  private static long START_TIME;
  private static long END_TIME;


  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    ASSET_NAME_V2 = "2";
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ASSET_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    ASSET_ADDRESS_V2 = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a7890";
    START_TIME = DateTime.now().minusDays(1).getMillis();
    END_TIME = DateTime.now().getMillis();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
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
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(getAssetIssueContract());
    assetIssueCapsule.setId("1");
    chainBaseManager
        .getAssetIssueStore()
        .put(
            ByteArray.fromString(ASSET_NAME),
            assetIssueCapsule);
    chainBaseManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString("1"),
            assetIssueCapsule);

    AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(getAssetIssueV2Contract());
    chainBaseManager
        .getAssetIssueV2Store()
        .put(
            ByteArray.fromString(ASSET_NAME_V2),
            assetIssueCapsuleV2);

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            0L);
    ownerCapsule.addAsset(ASSET_NAME.getBytes(), 100L);
    ownerCapsule.addAsset(ASSET_NAME_V2.getBytes(), 100L);

    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);

    AccountCapsule assetCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)),
            AccountType.AssetIssue,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());

    AccountCapsule assetCapsule2 =
        new AccountCapsule(
            ByteString.copyFromUtf8("asset2"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)),
            AccountType.AssetIssue,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());

    chainBaseManager.getAccountStore().reset();
    chainBaseManager.getAccountAssetStore().reset();
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    chainBaseManager.getAccountStore()
        .put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
    chainBaseManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);
    chainBaseManager.getAccountStore().put(assetCapsule2.getAddress().toByteArray(), assetCapsule2);

    chainBaseManager.getAccountAssetStore().put(ownerCapsule.getAddress().toByteArray(),
            new AccountAssetCapsule(ownerCapsule.getAddress()));
  }

  private TransferAssetContract getTransferAssetContract() {
    return TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private TransferAssetContract getTransferAssetV2Contract() {
    return TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME_V2)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private AssetIssueContract getAssetIssueContract() {
    return AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private AssetIssueContract getAssetIssueV2Contract() {
    return AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS_V2)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME_V2))
        .setId(ASSET_NAME_V2)
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

  private void initAssetIssue(long startTimestmp, long endTimestmp, String assetName) {
    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(assetName)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(startTimestmp)
            .setEndTime(endTimestmp)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    AccountCapsule toAccountCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(TO_ADDRESS));
    if (chainBaseManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
      chainBaseManager.getAssetIssueV2Store()
          .put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
      toAccountCapsule.addAssetV2(ByteArray.fromString(String.valueOf(id)), TOTAL_SUPPLY);
    } else {
      chainBaseManager.getAssetIssueStore()
          .put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
      toAccountCapsule.addAsset(assetName.getBytes(), TOTAL_SUPPLY);
    }
    chainBaseManager.getAccountStore()
        .put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }


  @Test
  public void testCreateNewAccount() throws Exception {
    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);
    TransferAssetContract transferAssetContract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(transferAssetContract);

    String NOT_EXISTS_ADDRESS =
        Wallet.getAddressPreFixString() + "008794500882809695a8a687866e76d4271a1abc";
    transferAssetContract = transferAssetContract.toBuilder()
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(NOT_EXISTS_ADDRESS))).build();

    org.apollo.protos.Protocol.Transaction.Contract contract =
        org.apollo.protos.Protocol.Transaction.Contract
            .newBuilder()
            .setType(Protocol.Transaction.Contract.ContractType.TransferAssetContract).setParameter(
            Any.pack(transferAssetContract)).build();

    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    chainBaseManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    Assert.assertEquals(true, processor.contractCreateNewAccount(contract));
    long bytes = trx.getSerializedSize();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    processor.consumeBandwidthForCreateNewAccount(ownerCapsule, bytes, 1526647838000L, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    long netUsage =
        bytes * chainBaseManager.getDynamicPropertiesStore().getCreateNewAccountBandwidthRate();
    Assert.assertEquals(
        netUsage,
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(netUsage, trace.getReceipt().getNetUsage());
  }


  @Test
  public void testFree() throws Exception {

    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeFreeTime());//slot
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(
        122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        chainBaseManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508882612L,
        chainBaseManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);
    ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestConsumeFreeTime()); // 508882612L + 28800L/2
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(61L + 122L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        chainBaseManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508897012L,
        chainBaseManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());
  }


  @Test
  public void testConsumeAssetAccount() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    chainBaseManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule assetCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));
    assetCapsule.setFrozen(10_000_000L, 0L);
    chainBaseManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule assetCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(508882612L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L,
        ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(
        122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(
        122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(
        122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        assetCapsuleNew.getNetUsage());

    Assert.assertEquals(0L, ret.getFee());

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    assetCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(508897012L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(61L + 122L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(61L + 122L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2("1"));
    Assert.assertEquals(61L + 122L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        assetCapsuleNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeAssetAccountV2() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    chainBaseManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetV2Contract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    // issuer freeze balance for bandwidth
    AccountCapsule issuerCapsuleV2 = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));
    issuerCapsuleV2.setFrozen(10_000_000L, 0L);
    chainBaseManager.getAccountStore().put(issuerCapsuleV2.getAddress().toByteArray(),
        issuerCapsuleV2);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule issuerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(508882612L, issuerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L,
        ownerCapsuleNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(
        113L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        issuerCapsuleNew.getNetUsage());
    Assert.assertEquals(
        113L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));

    Assert.assertEquals(508882612L,
        ownerCapsuleNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(0L, ret.getFee());

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    issuerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS_V2));

    Assert.assertEquals(508897012L, issuerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestAssetOperationTimeV2(ASSET_NAME_V2));
    Assert.assertEquals(56L + 113L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsageV2(ASSET_NAME_V2));
    Assert.assertEquals(56L + 113L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        issuerCapsuleNew.getNetUsage());
    Assert.assertEquals(0L, ret.getFee());

  }

  @Test
  public void testConsumeOwner() throws Exception {
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    chainBaseManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    AccountAssetCapsule ownerAssetCapsule = chainBaseManager.getAccountAssetStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountCapsule assetCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(
        122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, trace);

    ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122L
            + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

  }


  @Test
  public void testUsingFee() throws Exception {

    Args.getInstance().getGenesisBlock().getAssets().forEach(account -> {
      AccountCapsule capsule =
          new AccountCapsule(
              ByteString.copyFromUtf8(""),
              ByteString.copyFrom(account.getAddress()),
              AccountType.AssetIssue,
              100L);
      chainBaseManager.getAccountStore().put(account.getAddress(), capsule);
    });

    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    chainBaseManager.getDynamicPropertiesStore().saveFreeNetLimit(0L);

    TransferAssetContract contract = getTransferAssetContract();
    TransactionCapsule trx = new TransactionCapsule(contract);

    AccountCapsule ownerCapsule = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setBalance(10_000_000L);

    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());
    dbManager.consumeBandwidth(trx, trace);

    AccountCapsule ownerCapsuleNew = chainBaseManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long transactionFee =
        (122L + (chainBaseManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX
            : 0)) * chainBaseManager
            .getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(transactionFee,
        chainBaseManager.getDynamicPropertiesStore().getTotalTransactionCost());
    Assert.assertEquals(
        10_000_000L - transactionFee,
        ownerCapsuleNew.getBalance());
    Assert.assertEquals(transactionFee, trace.getReceipt().getNetFee());

    chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    dbManager.consumeBandwidth(trx, trace);
  }

  /**
   * sameTokenName close, consume success assetIssueCapsule.getOwnerAddress() !=
   * fromAccount.getAddress()) contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameCloseConsumeSuccess() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    chainBaseManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .setPublicFreeAssetNetLimit(2000)
            .setFreeAssetNetLimit(2000)
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    // V1
    chainBaseManager.getAssetIssueStore().put(assetIssueCapsule.createDbKey(), assetIssueCapsule);
    // V2
    chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
        assetIssueCapsule);

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    chainBaseManager.getAccountStore().put(toAddressCapsule.getAddress().toByteArray(),
        toAddressCapsule);

    TransferAssetContract contract = TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionCapsule trx = new TransactionCapsule(contract);
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize()
        + Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      //V1
      AssetIssueCapsule assetIssueCapsuleV1 =
          chainBaseManager.getAssetIssueStore().get(assetIssueCapsule.createDbKey());
      Assert.assertNotNull(assetIssueCapsuleV1);
      Assert.assertEquals(assetIssueCapsuleV1.getPublicFreeAssetNetUsage(), byteSize);

      AccountCapsule fromAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

      AccountCapsule ownerAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
          chainBaseManager.getAssetIssueV2Store().get(assetIssueCapsule.createDbV2Key());
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetUsage(), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsage(ASSET_NAME), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      chainBaseManager.getAssetIssueStore().delete(assetIssueCapsule.createDbKey());
      chainBaseManager.getAssetIssueV2Store().delete(assetIssueCapsule.createDbV2Key());
    }
  }

  /**
   * sameTokenName open, consume success assetIssueCapsule.getOwnerAddress() !=
   * fromAccount.getAddress()) contract.getType() = TransferAssetContract
   */
  @Test
  public void sameTokenNameOpenConsumeSuccess() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    long id = chainBaseManager.getDynamicPropertiesStore().getTokenIdNum() + 1;
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(id);
    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
            .setId(Long.toString(id))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(START_TIME)
            .setEndTime(END_TIME)
            .setVoteScore(VOTE_SCORE)
            .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
            .setPublicFreeAssetNetLimit(2000)
            .setFreeAssetNetLimit(2000)
            .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    // V2
    chainBaseManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(),
        assetIssueCapsule);

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    chainBaseManager.getAccountStore().put(toAddressCapsule.getAddress().toByteArray(),
        toAddressCapsule);

    TransferAssetContract contract = TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(String.valueOf(id))))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionCapsule trx = new TransactionCapsule(contract);
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize()
        + Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);

    try {
      processor.consume(trx, trace);
      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountCapsule ownerAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertNotNull(ownerAccount);
      Assert.assertEquals(ownerAccount.getNetUsage(), byteSize);

      //V2
      AssetIssueCapsule assetIssueCapsuleV2 =
          chainBaseManager.getAssetIssueV2Store().get(assetIssueCapsule.createDbV2Key());
      Assert.assertNotNull(assetIssueCapsuleV2);
      Assert.assertEquals(assetIssueCapsuleV2.getPublicFreeAssetNetUsage(), byteSize);

      AccountCapsule fromAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);
      Assert.assertEquals(fromAccount.getFreeAssetNetUsageV2(String.valueOf(id)), byteSize);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
      chainBaseManager.getAssetIssueStore().delete(assetIssueCapsule.createDbKey());
      chainBaseManager.getAssetIssueV2Store().delete(assetIssueCapsule.createDbV2Key());
    }
  }

  /**
   * sameTokenName close, consume success contract.getType() = TransferContract toAddressAccount
   * isn't exist.
   */
  @Test
  public void sameTokenNameCloseTransferToAccountNotExist() {
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(0);
    chainBaseManager.getDynamicPropertiesStore().saveTotalNetWeight(10_000_000L);

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    ownerCapsule.setBalance(10_000_000L);
    long expireTime = DateTime.now().getMillis() + 6 * 86_400_000;
    ownerCapsule.setFrozenForBandwidth(2_000_000L, expireTime);
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAddressCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            chainBaseManager.getDynamicPropertiesStore().getAssetIssueFee());
    toAddressCapsule.setBalance(10_000_000L);
    long expireTime2 = DateTime.now().getMillis() + 6 * 86_400_000;
    toAddressCapsule.setFrozenForBandwidth(2_000_000L, expireTime2);
    chainBaseManager.getAccountStore().delete(toAddressCapsule.getAddress().toByteArray());

    TransferContract contract = TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();

    TransactionCapsule trx = new TransactionCapsule(contract, chainBaseManager.getAccountStore());
    TransactionTrace trace = new TransactionTrace(trx, StoreFactory
        .getInstance(), new RuntimeImpl());

    long byteSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize()
        + Constant.MAX_RESULT_SIZE_IN_TX;

    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);

    try {
      processor.consume(trx, trace);

      Assert.assertEquals(trace.getReceipt().getNetFee(), 0);
      Assert.assertEquals(trace.getReceipt().getNetUsage(), byteSize);
      AccountCapsule fromAccount =
          chainBaseManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(fromAccount);
      Assert.assertEquals(fromAccount.getNetUsage(), byteSize);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (TooBigTransactionResultException e) {
      Assert.assertFalse(e instanceof TooBigTransactionResultException);
    } catch (AccountResourceInsufficientException e) {
      Assert.assertFalse(e instanceof AccountResourceInsufficientException);
    } finally {
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS));
      chainBaseManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    }
  }
}
