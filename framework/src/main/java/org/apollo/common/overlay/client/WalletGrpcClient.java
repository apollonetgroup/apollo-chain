package org.apollo.common.overlay.client;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apollo.api.WalletGrpc;
import org.apollo.api.GrpcAPI.AssetIssueList;
import org.apollo.api.GrpcAPI.BytesMessage;
import org.apollo.api.GrpcAPI.EmptyMessage;
import org.apollo.api.GrpcAPI.NodeList;
import org.apollo.api.GrpcAPI.NumberMessage;
import org.apollo.api.GrpcAPI.Return;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.Block;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.apollo.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.apollo.protos.contract.BalanceContract.TransferContract;
import org.apollo.protos.contract.WitnessContract.VoteWitnessContract;
import org.apollo.protos.contract.WitnessContract.WitnessCreateContract;

public class WalletGrpcClient {

  private final ManagedChannel channel;
  private final WalletGrpc.WalletBlockingStub walletBlockingStub;

  public WalletGrpcClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
    walletBlockingStub = WalletGrpc.newBlockingStub(channel);
  }

  public WalletGrpcClient(String host) {
    channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext(true)
        .build();
    walletBlockingStub = WalletGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public Account queryAccount(byte[] address) {
    ByteString addressByteString = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressByteString).build();
    return walletBlockingStub.getAccount(request);
  }

  public Transaction createTransaction(TransferContract contract) {
    return walletBlockingStub.createTransaction(contract);
  }

  public Transaction createTransferAssetTransaction(TransferAssetContract contract) {
    return walletBlockingStub.transferAsset(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      ParticipateAssetIssueContract contract) {
    return walletBlockingStub.participateAssetIssue(contract);
  }

  public Transaction createAssetIssue(AssetIssueContract contract) {
    return walletBlockingStub.createAssetIssue(contract);
  }

  public Transaction voteWitnessAccount(VoteWitnessContract contract) {
    return walletBlockingStub.voteWitnessAccount(contract);
  }

  public Transaction createWitness(WitnessCreateContract contract) {
    return walletBlockingStub.createWitness(contract);
  }

  public boolean broadcastTransaction(Transaction signedTransaction) {
    Return response = walletBlockingStub.broadcastTransaction(signedTransaction);
    return response.getResult();
  }

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      return walletBlockingStub.getNowBlock(EmptyMessage.newBuilder().build());
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return walletBlockingStub.getBlockByNum(builder.build());
  }

  public Optional<NodeList> listNodes() {
    NodeList nodeList = walletBlockingStub
        .listNodes(EmptyMessage.newBuilder().build());
    if (nodeList != null) {
      return Optional.of(nodeList);
    }
    return Optional.empty();
  }

  public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
    ByteString addressByteString = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressByteString).build();
    AssetIssueList assetIssueList = walletBlockingStub
        .getAssetIssueByAccount(request);
    if (assetIssueList != null) {
      return Optional.of(assetIssueList);
    }
    return Optional.empty();
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return walletBlockingStub.getAssetIssueByName(request);
  }

  public Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();

    AssetIssueList assetIssueList = walletBlockingStub
        .getAssetIssueListByName(request);
    if (assetIssueList != null) {
      return Optional.of(assetIssueList);
    }
    return Optional.empty();
  }

  public AssetIssueContract getAssetIssueById(String assetId) {
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    return walletBlockingStub.getAssetIssueById(request);
  }

}
