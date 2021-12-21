package stest.apollo.wallet.committee;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.WalletGrpc;
import org.apollo.api.WalletSolidityGrpc;
import org.apollo.api.GrpcAPI.BytesMessage;
import org.apollo.api.GrpcAPI.EmptyMessage;
import org.apollo.api.GrpcAPI.ProposalList;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.ChainParameters;
import org.apollo.protos.Protocol.Proposal;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.Parameter.CommonConstant;
import stest.apollo.wallet.common.client.utils.PublicMethed;


@Slf4j
public class WalletTestCommittee004 {

  private static final long now = System.currentTimeMillis();
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  //private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  //private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  //private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void test1DeleteProposal() {
    PublicMethed.sendcoin(witness001Address, 1000000L,
        toAddress, testKey003, blockingStubFull);
    PublicMethed.sendcoin(witness002Address, 1000000L,
        toAddress, testKey003, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Create a proposal and approval it
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 99999L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address, witnessKey001,
        proposalId, true, blockingStubFull));
    logger.info(Integer.toString(listProposals.get().getProposals(0).getStateValue()));
    //The state is "pending", state value == 0
    Assert.assertTrue(listProposals.get().getProposals(0).getStateValue() == 0);

    //When the proposal isn't created by you, you can't delete it.
    Assert.assertFalse(PublicMethed.deleteProposal(witness002Address, witnessKey002,
        proposalId, blockingStubFull));
    //Cancel the proposal
    Assert.assertTrue(PublicMethed.deleteProposal(witness001Address, witnessKey001,
        proposalId, blockingStubFull));
    //When the state is cancel, you can't delete it again.
    Assert.assertFalse(PublicMethed.deleteProposal(witness001Address, witnessKey001,
        proposalId, blockingStubFull));
    //You can't delete an invalid proposal
    Assert.assertFalse(PublicMethed.deleteProposal(witness001Address, witnessKey001,
        proposalId + 100, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getStateValue()));
    //The state is "cancel", state value == 3
    Assert.assertTrue(listProposals.get().getProposals(0).getStateValue() == 3);

    //When the state is cancel, you can't approval proposal
    Assert.assertFalse(PublicMethed.approveProposal(witness001Address, witnessKey001,
        proposalId, true, blockingStubFull));
    Assert.assertFalse(PublicMethed.approveProposal(witness001Address, witnessKey001,
        proposalId, false, blockingStubFull));
  }

  @Test(enabled = true)
  public void test2GetProposal() {
    //Create a proposal and approval it
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 999999999L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();

    BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(proposalId.toString()))))
        .build();
    Proposal proposal = blockingStubFull.getProposalById(request);
    Optional<Proposal> getProposal = Optional.ofNullable(proposal);

    Assert.assertTrue(getProposal.isPresent());
    Assert.assertTrue(getProposal.get().getStateValue() == 0);

    //Invalid get proposal
    final Integer wrongProposalId = proposalId + 99;
    request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(wrongProposalId.toString()))))
        .build();
    proposal = blockingStubFull.getProposalById(request);
    getProposal = Optional.ofNullable(proposal);
    logger.info(Long.toString(getProposal.get().getCreateTime()));
    Assert.assertTrue(getProposal.get().getCreateTime() == 0);
  }

  @Test(enabled = false)
  public void testGetChainParameters() {
    //Set the default map
    HashMap<String, Long> defaultCommitteeMap = new HashMap<String, Long>();
    defaultCommitteeMap.put("MAINTENANCE_TIME_INTERVAL", 300000L);
    defaultCommitteeMap.put("ACCOUNT_UPGRADE_COST", 9999000000L);
    defaultCommitteeMap.put("CREATE_ACCOUNT_FEE", 100000L);
    defaultCommitteeMap.put("TRANSACTION_FEE", 10L);
    defaultCommitteeMap.put("ASSET_ISSUE_FEE", 1024000000L);
    defaultCommitteeMap.put("WITNESS_PAY_PER_BLOCK", 32000000L);
    defaultCommitteeMap.put("WITNESS_STANDBY_ALLOWANCE", 115200000000L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT", 0L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_BANDWIDTH_RATE", 1L);

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }
    Assert.assertTrue(getChainParameters.get().getChainParameterCount() >= 10);
    Assert.assertTrue(getChainParameters.get()
        .getChainParameter(1).getValue() == 9999000000L);
    Assert.assertTrue(getChainParameters.get().getChainParameter(4)
        .getValue() == 1024000000L);
    Assert.assertTrue(getChainParameters.get().getChainParameter(7).getValue() == 0);
    Assert.assertTrue(getChainParameters.get().getChainParameter(8).getValue() == 1);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


