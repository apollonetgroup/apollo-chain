package org.apollo.common.overlay.message;

import com.google.protobuf.ByteString;

import lombok.Getter;

import org.apollo.common.overlay.discover.node.Node;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.config.args.Args;
import org.apollo.core.net.message.MessageTypes;
import org.apollo.protos.Protocol;
import org.apollo.protos.Discover.Endpoint;
import org.apollo.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

  @Getter
  private Protocol.HelloMessage helloMessage;

  public HelloMessage(byte type, byte[] rawData) throws Exception {
    super(type, rawData);
    this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
  }

  public HelloMessage(Node from, long timestamp, BlockCapsule.BlockId genesisBlockId,
      BlockCapsule.BlockId solidBlockId, BlockCapsule.BlockId headBlockId) {

    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setNodeId(ByteString.copyFrom(from.getId()))
        .setPort(from.getPort())
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .build();

    Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(genesisBlockId.getByteString())
        .setNumber(genesisBlockId.getNum())
        .build();

    Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(solidBlockId.getByteString())
        .setNumber(solidBlockId.getNum())
        .build();

    Protocol.HelloMessage.BlockId hBlockId = Protocol.HelloMessage.BlockId.newBuilder()
        .setHash(headBlockId.getByteString())
        .setNumber(headBlockId.getNum())
        .build();

    Builder builder = Protocol.HelloMessage.newBuilder();

    builder.setFrom(fromEndpoint);
    builder.setVersion(Args.getInstance().getNodeP2pVersion());
    builder.setTimestamp(timestamp);
    builder.setGenesisBlockId(gBlockId);
    builder.setSolidBlockId(sBlockId);
    builder.setHeadBlockId(hBlockId);

    this.helloMessage = builder.build();
    this.type = MessageTypes.P2P_HELLO.asByte();
    this.data = this.helloMessage.toByteArray();
  }

  public void setHelloMessage(Protocol.HelloMessage helloMessage) {
    this.helloMessage = helloMessage;
    this.data = this.helloMessage.toByteArray();
  }

  public int getVersion() {
    return this.helloMessage.getVersion();
  }

  public long getTimestamp() {
    return this.helloMessage.getTimestamp();
  }

  public Node getFrom() {
    Endpoint from = this.helloMessage.getFrom();
    return new Node(from.getNodeId().toByteArray(),
        ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
  }

  public BlockCapsule.BlockId getGenesisBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getGenesisBlockId().getHash(),
        this.helloMessage.getGenesisBlockId().getNumber());
  }

  public BlockCapsule.BlockId getSolidBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getSolidBlockId().getHash(),
        this.helloMessage.getSolidBlockId().getNumber());
  }

  public BlockCapsule.BlockId getHeadBlockId() {
    return new BlockCapsule.BlockId(this.helloMessage.getHeadBlockId().getHash(),
        this.helloMessage.getHeadBlockId().getNumber());
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(helloMessage.toString()).toString();
  }

}