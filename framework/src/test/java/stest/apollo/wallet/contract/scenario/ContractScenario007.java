package stest.apollo.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AccountResourceMessage;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.contract.SmartContractOuterClass.SmartContract;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract007Address = ecKey1.getAddress();
  String contract007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

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
    PublicMethed.printAddress(contract007Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void deployErc721CardMigration() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract007Address = ecKey1.getAddress();
    contract007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(contract007Address, 20000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract007Address, 100000000L,
        3, 1, contract007Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract007Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Account account = PublicMethed.queryAccount(contract007Key, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "ERC721Token";

    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario007_deployErc721CardMigration");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario007_deployErc721CardMigration");
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contract007Key, contract007Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    accountResource = PublicMethed.getAccountResource(contract007Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    account = PublicMethed.queryAccount(contract007Key, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


