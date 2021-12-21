package org.apollo.consensus.pbft.message;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.stream.Collectors;

import org.apollo.common.crypto.ECKey;
import org.apollo.common.overlay.message.Message;
import org.apollo.common.utils.ByteUtil;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.capsule.TransactionCapsule;
import org.apollo.core.exception.P2pException;
import org.apollo.protos.Protocol.PBFTMessage;
import org.apollo.protos.Protocol.SRL;
import org.apollo.protos.Protocol.PBFTMessage.DataType;
import org.bouncycastle.util.encoders.Hex;

public abstract class PbftBaseMessage extends Message {

  protected PBFTMessage pbftMessage;

  private boolean isSwitch;

  private byte[] publicKey;

  public PbftBaseMessage() {
  }

  public PbftBaseMessage(byte type, byte[] data) throws IOException, P2pException {
    super(type, data);
    this.pbftMessage = PBFTMessage.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, pbftMessage.toByteArray());
    }
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public PBFTMessage getPbftMessage() {
    return pbftMessage;
  }

  public PbftBaseMessage setPbftMessage(PBFTMessage pbftMessage) {
    this.pbftMessage = pbftMessage;
    return this;
  }

  public boolean isSwitch() {
    return isSwitch;
  }

  public PbftBaseMessage setSwitch(boolean aSwitch) {
    isSwitch = aSwitch;
    return this;
  }

  public PbftBaseMessage setData(byte[] data) {
    this.data = data;
    return this;
  }

  public PbftBaseMessage setType(byte type) {
    this.type = type;
    return this;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public String getKey() {
    return getNo() + "_" + Hex.toHexString(publicKey);
  }

  public String getDataKey() {
    return getNo() + "_" + Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  public long getNumber() {
    return pbftMessage.getRawData().getViewN();
  }

  public long getEpoch() {
    return pbftMessage.getRawData().getEpoch();
  }

  public DataType getDataType() {
    return pbftMessage.getRawData().getDataType();
  }

  public abstract String getNo();

  public void analyzeSignature() throws SignatureException {
    byte[] hash = Sha256Hash.hash(true, getPbftMessage().getRawData().toByteArray());
    publicKey = ECKey.signatureToAddress(hash, TransactionCapsule
        .getBase64FromByteString(getPbftMessage().getSignature()));
  }

  @Override
  public String toString() {
    return "DataType:" + getDataType() + ", MsgType:" + pbftMessage.getRawData().getMsgType()
        + ", node address:" + (ByteUtil.isNullOrZeroArray(publicKey) ? null
        : Hex.toHexString(publicKey))
        + ", viewN:" + pbftMessage.getRawData().getViewN()
        + ", epoch:" + pbftMessage.getRawData().getEpoch()
        + ", data:" + getDataString()
        + ", " + super.toString();
  }

  public String getDataString() {
    return getDataType() == DataType.SRL ? decode()
        : Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  private String decode() {
    try {
      SRL srList = SRL.parseFrom(pbftMessage.getRawData().getData().toByteArray());
      return "sr list = " + srList.getSrAddressList().stream().map(
          bytes -> StringUtil.encode58Check(bytes.toByteArray())).collect(Collectors.toList());
    } catch (InvalidProtocolBufferException e) {
    }
    return "decode error";
  }
}
