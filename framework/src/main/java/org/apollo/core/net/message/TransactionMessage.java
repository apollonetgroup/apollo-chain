package org.apollo.core.net.message;

import org.apollo.common.overlay.message.Message;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.core.capsule.TransactionCapsule;
import org.apollo.core.net.message.MessageTypes;
import org.apollo.protos.Protocol.Transaction;

public class TransactionMessage extends TronMessage {

  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws Exception {
    super(data);
    this.transactionCapsule = new TransactionCapsule(getCodedInputStream(data));
    this.type = MessageTypes.TRX.asByte();
    if (Message.isFilter()) {
      compareBytes(data, transactionCapsule.getInstance().toByteArray());
      transactionCapsule
          .validContractProto(transactionCapsule.getInstance().getRawData().getContract(0));
    }
  }

  public TransactionMessage(Transaction trx) {
    this.transactionCapsule = new TransactionCapsule(trx);
    this.type = MessageTypes.TRX.asByte();
    this.data = trx.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.transactionCapsule.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionCapsule getTransactionCapsule() {
    return this.transactionCapsule;
  }
}
