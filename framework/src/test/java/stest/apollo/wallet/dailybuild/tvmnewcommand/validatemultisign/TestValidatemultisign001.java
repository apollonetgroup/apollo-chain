package stest.apollo.wallet.dailybuild.tvmnewcommand.validatemultisign;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AccountResourceMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.ByteUtil;
import org.apollo.common.utils.StringUtil;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.Protocol.TransactionInfo;
import org.apollo.protos.contract.SmartContractOuterClass.SmartContract;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.apollo.wallet.common.client.utils.Sha256Hash;
import stest.apollo.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class TestValidatemultisign001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey001.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey002.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  ECKey ecKey003 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey003.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey003.getPrivKeyBytes());
  ECKey ecKey004 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey004.getAddress();
  String participateKey = ByteArray.toHexString(ecKey004.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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

    PublicMethed.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "Deploy validatemultisign contract")
  public void test001DeployContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 100_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(dev001Key, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = accountResource.getEnergyUsed();
    Long beforeNetUsed = accountResource.getNetUsed();
    Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/validatemultisign001.sol";
    String contractName = "validatemultisignTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    PublicMethed.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Trigger validatemultisign contract with "
      + "Permission(address) case")
  public void test002validatemultisign() {
    List<Object> signatures = new ArrayList<>();

    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinGetTransaction(
        fromAddress, 1L, ownerAddress, ownerKey, blockingStubFull, ownerKeyString);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(0), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    // Trigger with correct Permission address
    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        0, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with wrong Permission address
    merged = ByteUtil.merge(dev001Address, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(dev001Address),
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with address that have not permission
    merged = ByteUtil.merge(fromAddress, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(fromAddress),
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with not exist address
    merged = ByteUtil.merge(manager1Address, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(manager1Address),
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with error format address
    merged = ByteUtil.merge(manager1Address, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList("TVgXWwGWE9huXiE4FuzDuGnCPUowsbZ8VZ",
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Trigger validatemultisign contract with "
      + "Permission(permissionId) case")
  public void test003validatemultisign() {
    List<Object> signatures = new ArrayList<>();

    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinGetTransaction(
        fromAddress, 1L, ownerAddress, ownerKey, blockingStubFull, ownerKeyString);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    // Trigger with wrong PermissionID
    long permissionId = 2;

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromLong(permissionId), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with error format PermissionID
    permissionId = 100;
    merged = ByteUtil.merge(ownerAddress, ByteArray.fromLong(permissionId), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with Long.MAX_VALUE + 1 PermissionID
    permissionId = Long.MAX_VALUE + 1;
    merged = ByteUtil.merge(ownerAddress, ByteArray.fromLong(permissionId), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with Long.MIN_VALUE - 1 PermissionID
    permissionId = Long.MIN_VALUE - 1;
    merged = ByteUtil.merge(ownerAddress, ByteArray.fromLong(permissionId), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Trigger validatemultisign contract with "
      + "Permission(hash) case")
  public void test004validatemultisign() {
    List<Object> signatures = new ArrayList<>();

    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, ownerAddress, 0, ownerKey, blockingStubFull);
    transaction = TransactionUtils.setTimestamp(transaction);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(0), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    // Trigger with no sign hash
    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        0, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with wrong hash
    transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, ownerAddress, 0, ownerKey, blockingStubFull);
    logger.info("hash: {}", Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes());

    hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    transaction = TransactionUtils.setTimestamp(transaction);
    hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // 1) address B create transaction_1, but address A`s permission address sign
    // 2) user address A verify transaction_1 that created by B
    transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, dev001Address, 0, dev001Key, blockingStubFull);
    transaction = TransactionUtils.setTimestamp(transaction);

    hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(0), hash);
    tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    signatures.clear();
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        0, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

}
