package org.apollo.common.overlay.message;

import org.apollo.core.net.message.MessageTypes;
import org.bouncycastle.util.encoders.Hex;

public class PongMessage extends P2pMessage {

  private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

  public PongMessage() {
    this.type = MessageTypes.P2P_PONG.asByte();
    this.data = FIXED_PAYLOAD;
  }

  public PongMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }

  @Override
  public byte[] getData() {
    return FIXED_PAYLOAD;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }
}
