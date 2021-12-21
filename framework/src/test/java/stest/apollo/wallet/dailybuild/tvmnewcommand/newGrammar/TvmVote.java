package stest.apollo.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.contract.SmartContractOuterClass;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter;
import stest.apollo.wallet.common.client.utils.Base58;
import stest.apollo.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TvmVote {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);
  byte[] mapKeyContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  int freezeCount = 100000000;
  int voteCount = 1;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/tvmVote.sol";
    String contractName = "TestVote";

    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = false, description = "freeze balance and vote witness")
  public void yty() {
    String filePath = "src/test/resources/soliditycode/tvmVote.sol";
    String contractName = "TestVote";

    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0, 0, 10000, "0", 0,
            null, contractExcKey, contractExcAddress,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }

    mapKeyContract = infoById.get().getContractAddress().toByteArray();

    String methodStr = "freeze(address,uint256,uint256)";
    String receiverAdd = Base58.encode58Check(mapKeyContract);
    String args = "\"" + receiverAdd + "\"," + freezeCount + ",1";
    logger.info("receiverAdd: " + receiverAdd);
    logger.info("args: " + args);
    String triggerTxid = PublicMethed
        .triggerContract(mapKeyContract, methodStr, args, false, 0,
            1000000000L, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("infoById:  " + infoById.toString());


  }

  @Test(enabled = false, description = "query reward balance")
  public void test01QueryRewardBalance() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryRewardBalance()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    long trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info("result: " + trueRes);
    Assert.assertEquals(0, trueRes);

    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(mapKeyContract))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();
    org.testng.Assert.assertEquals(trueRes, reward);
  }


  @Test(enabled = false, description = "freeze balance and vote witness")
  public void test02VoteWitness() {
    String methodStr = "freeze(address,uint256,uint256)";
    String receiverAdd = Base58.encode58Check(mapKeyContract);
    String args = "\"" + receiverAdd + "\"," + freezeCount + ",1";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract,
        methodStr, args, false, 0, maxFeeLimit, "0", 0,
        contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
    String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("freezeForEnergy", note);
    Assert.assertEquals(freezeCount, internal.getCallValueInfo(0).getCallValue());

    String witness58Add = Base58.encode58Check(witnessAddress);
    args = "[\"" + witness58Add + "\"],[" + voteCount + "]";
    logger.info("vote args: " + args);
    methodStr = "voteWitness(address[],uint256[])";
    triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    internal = transactionInfo.get().getInternalTransactions(0);
    note = internal.getNote().toStringUtf8();
    Assert.assertEquals("voteWitness", note);
    Assert.assertTrue(internal.getExtra().length() > 1);

    Protocol.Account info = PublicMethed.queryAccount(mapKeyContract, blockingStubFull);
    int voteCount = info.getVotesCount();
    logger.info("voteCount: " + voteCount);
    Assert.assertEquals(1, voteCount);
  }

  @Test(enabled = false, description = "query contract address is Sr Candidate or not")
  public void test03IsSrCandidate() {
    String args = "\"" + Base58.encode58Check(mapKeyContract) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, 0);
  }

  @Test(enabled = false, description = "query sr address is Sr Candidate or not")
  public void test04IsSrCandidate() {
    String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(1, 1);
  }

  @Test(enabled = false, description = "query zero address is Sr Candidate or not")
  public void test05IsSrCandidate() {
    String args = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "isWitness(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, 0);
  }

  @Test(enabled = false, description = "query sr's total vote count")
  public void test06querySrTotalVoteCount() {
    String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryTotalVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(0, trueRes);
  }

  @Test(enabled = false, description = "query contract's total vote count")
  public void test07queryContractTotalVoteCount() {
    String args = "\"" + Base58.encode58Check(mapKeyContract) + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryTotalVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(freezeCount / 1000000, trueRes);
  }

  @Test(enabled = false, description = "query vote count")
  public void test08queryVoteCount() {
    String from = Base58.encode58Check(mapKeyContract);
    String to = Base58.encode58Check(witnessAddress);
    String args = "\"" + from + "\",\"" + to + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryVoteCount(address,address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(voteCount, trueRes);
  }

  @Test(enabled = false, description = "query contract used vote count")
  public void test09queryUsedVoteCount() {
    String from = Base58.encode58Check(mapKeyContract);
    String args = "\"" + from + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryUsedVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertEquals(voteCount, trueRes);
  }

  @Test(enabled = false, description = "query witnesses received vote count")
  public void test10queryReceivedVoteCount() {
    String witness = Base58.encode58Check(witnessAddress);
    String args = "\"" + witness + "\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "queryReceivedVoteCount(address)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    logger.info(trueRes + "");
    Assert.assertTrue(trueRes > 0);
    Optional<GrpcAPI.WitnessList> list = PublicMethed.listWitnesses(blockingStubFull);
    long receiveCount = 0;
    String temAdd;
    for (int i = 0; i < list.get().getWitnessesCount(); i++) {
      temAdd = Base58.encode58Check(list.get().getWitnesses(i).getAddress().toByteArray());
      if (witness.equals(temAdd)) {
        receiveCount = list.get().getWitnesses(i).getVoteCount();
        break;
      }
    }
    Assert.assertEquals(trueRes, receiveCount);
  }

  @Test(enabled = false, description = "withdraw reward")
  public void test11WithdrawReward() {
    String methodStr = "withdrawReward()";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
    String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("withdrawReward", note);
    Assert.assertEquals(1, internal.getCallValueInfoCount());
    Assert.assertEquals("", internal.getCallValueInfo(0).toString());
  }

  @Test(enabled = false, description = "unfreeze energy")
  public void test12Unfreeze() {
    String methodStr = "unfreeze(address,uint256)";
    String args = "\"" + Base58.encode58Check(mapKeyContract) + "\",1";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());

    Protocol.InternalTransaction internal = transactionInfo.get().getInternalTransactions(0);
    String note = internal.getNote().toStringUtf8();
    Assert.assertEquals("unfreezeForEnergy", note);
    Assert.assertEquals(freezeCount, internal.getCallValueInfo(0).getCallValue());
  }

  @Test(enabled = false, description = "kill me")
  public void test13Suicide() {
    String methodStr = "killme(address)";
    String args = "\"" + Base58.encode58Check(witnessAddress) + "\"";
    String triggerTxid = PublicMethed.triggerContract(mapKeyContract, methodStr, args, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed
        .getContract(mapKeyContract, blockingStubFull);
    Assert.assertEquals("", smartContract.getAbi().toString());
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

