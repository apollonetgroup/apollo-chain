package stest.apollo.wallet.other;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.TransactionInfo;
import org.apollo.protos.contract.SmartContractOuterClass.SmartContract;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.WalletClient;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.Base58;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class deploySideGateway {


  private final String testDepositTrx = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096"
      + "c6bacb7";
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositTrx);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] depositAddress = ecKey1.getAddress();
  String testKeyFordeposit = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = "127.0.0.1:50151";

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
  }

  @Test(enabled = true, description = "deploy Side Chain Gateway")
  public void test1DepositTrc20001() {

    PublicMethed.printAddress(testKeyFordeposit);

    String contractName = "gateWaysidechainContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_SideGateway");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_SideGateway");
    String parame = "\"" + Base58.encode58Check(testDepositAddress) + "\"";

    String deployTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address)",
            parame, "",
            maxFeeLimit,
            0L, 100, null, testKeyFordeposit, depositAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    byte[] mainChainGateway = infoById.get().getContractAddress().toByteArray();
    String mainChainGatewayAddress = WalletClient.encode58Check(mainChainGateway);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertNotNull(mainChainGateway);

    SmartContract smartContract = PublicMethed.getContract(mainChainGateway,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    String outputPath = "./src/test/resources/sideChainGatewayAddress";
    try {
      File mainChainFile = new File(outputPath);
      Boolean cun = mainChainFile.createNewFile();
      FileWriter writer = new FileWriter(mainChainFile);
      BufferedWriter out = new BufferedWriter(writer);
      out.write(mainChainGatewayAddress);

      out.close();
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

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
