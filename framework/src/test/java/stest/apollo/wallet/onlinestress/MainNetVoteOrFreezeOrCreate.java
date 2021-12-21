package stest.apollo.wallet.onlinestress;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apollo.api.GrpcAPI;
import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.api.GrpcAPI.Return;
import org.apollo.api.GrpcAPI.WitnessList;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.contract.BalanceContract;
import org.apollo.protos.contract.WitnessContract;
import org.bouncycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.WalletClient;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.Base58;
import stest.apollo.wallet.common.client.utils.PublicMethed;
import stest.apollo.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class MainNetVoteOrFreezeOrCreate {

  private static final long now = System.currentTimeMillis();
  private static String name = "mainNetAsset_" + Long.toString(now);
  //testng001、testng002、testng003、testng004
  //Devaccount
  private final String testKey001 =
      "2514B1DD2942FF07F68C2DDC0EE791BC7FBE96FDD95E89B7B9BB3B4C4770FFAC";
  //Zion
  private final String testKey002 =
      "56244EE6B33C14C46704DFB67ED5D2BBCBED952EE46F1FD88A50C32C8C5C64CE";
  //Default
  private final String defaultKey =
      //Mainet
      //"8DFBB4513AECF779A0803C7CEBF2CDCC51585121FAB1E086465C4E0B40724AF1";
      //Beta Env
      "6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] defaultAddress = PublicMethed.getFinalAddress(defaultKey);
  private final Long sendAmount = 1026000000L;
  long totalSupply = now;
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "f";
  String url = "h";
  Long startTime;
  Long endTime;
  Boolean ret = false;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long start;
  private Long end;
  private Long beforeToBalance;
  private Long afterToBalance;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /**
   * constructor.
   */

  public static Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Protocol.Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI
        .EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    BalanceContract.FreezeBalanceContract.Builder builder = BalanceContract.FreezeBalanceContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    BalanceContract.FreezeBalanceContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }
    return true;
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    startTime = System.currentTimeMillis();
  }

  //@Test(enabled = false)
  @Test(enabled = false, threadPoolSize = 2, invocationCount = 2)
  public void freezeAndSendcoin() throws InterruptedException {
    Random rand = new Random();
    Integer randNum = 0;
    randNum = rand.nextInt(1000);
    try {
      Thread.sleep(randNum);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    GrpcAPI.WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    Integer i = 0;
    while (i++ < 3) {
      ret = false;
      Integer waitTime = 10;
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] accountAddress = ecKey1.getAddress();
      String testKeyAccount = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      logger.info(Base58.encode58Check(accountAddress));
      logger.info(testKeyAccount);
      Integer tryTimes = 0;

      while (!ret) {
        ret = PublicMethed
            .createAccount(defaultAddress, accountAddress, defaultKey, blockingStubFull);
        logger.info("createAccount");

        if (tryTimes++ == 10) {
          break;
        }
      }

      ret = false;
      while (!ret) {
        ret = PublicMethed
            .sendcoin(accountAddress, sendAmount, defaultAddress, defaultKey, blockingStubFull);
        logger.info("sendcoin");
      }
      ret = false;
      while (!ret) {
        name = "mainNetAsset_" + Long.toString(System.currentTimeMillis());
        totalSupply = System.currentTimeMillis();
        start = System.currentTimeMillis() + 2000;
        end = System.currentTimeMillis() + 1000000000;
        ret = PublicMethed.createAssetIssue(accountAddress, name, totalSupply, 1, 1, start, end,
            1, description, url, 3000L, 3000L, 1L, 1L,
            testKeyAccount, blockingStubFull);
        logger.info("createAssetIssue");
      }
      ret = false;
      while (!ret) {
        ret = freezeBalance(accountAddress, 1000000L, 3, testKeyAccount,
            blockingStubFull);
        logger.info("freezeBalance");
      }
      /*      ret = false;
      while (!ret) {
        ret = PublicMethed
            .transferAsset(toAddress, name.getBytes(), 10L, accountAddress, testKeyAccount,
                blockingStubFull);
        logger.info("transferAsset");
      }*/
      ret = false;
      while (!ret) {
        String voteStr = Base58
            .encode58Check(result.get().getWitnesses(i % 5).getAddress().toByteArray());
        HashMap<String, String> smallVoteMap = new HashMap<String, String>();
        smallVoteMap.put(voteStr, "1");
        ret = voteWitness(smallVoteMap, accountAddress, testKeyAccount);
        logger.info("voteWitness");
      }
    }
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    endTime = System.currentTimeMillis();
    logger.info("Time is " + Long.toString(endTime - startTime));
    Account fromAccount = PublicMethed.queryAccount(testKey001, blockingStubFull);
    Account toAccount = PublicMethed.queryAccount(testKey002, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }

  /**
   * constructor.
   */

  public Boolean voteWitness(HashMap<String, String> witness, byte[] addRess, String priKey) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account beforeVote = queryAccount(ecKey, blockingStubFull);
    Long beforeVoteNum = 0L;
    if (beforeVote.getVotesCount() != 0) {
      beforeVoteNum = beforeVote.getVotes(0).getVoteCount();
    }

    WitnessContract.VoteWitnessContract.Builder builder = WitnessContract.VoteWitnessContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(addRess));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      final long count = Long.parseLong(value);
      WitnessContract.VoteWitnessContract.Vote.Builder voteBuilder = WitnessContract
          .VoteWitnessContract.Vote
          .newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      logger.info("address = " + ByteArray.toHexString(address));
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    WitnessContract.VoteWitnessContract contract = builder.build();

    Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      //logger.info("transaction == null,\n contract:{},\n transaction:{}" , contract.toString(),
      // transaction.toString());
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      //logger.info(response.getCode().toString());
      return false;
    }
    /*    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/
    return true;
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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}

