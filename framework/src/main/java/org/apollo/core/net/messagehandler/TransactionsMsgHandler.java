package org.apollo.core.net.messagehandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apollo.core.config.args.Args;
import org.apollo.core.exception.P2pException;
import org.apollo.core.exception.P2pException.TypeEnum;
import org.apollo.core.net.ApolloNetDelegate;
import org.apollo.core.net.message.TransactionMessage;
import org.apollo.core.net.message.TransactionsMessage;
import org.apollo.core.net.message.TronMessage;
import org.apollo.core.net.peer.Item;
import org.apollo.core.net.peer.PeerConnection;
import org.apollo.core.net.service.AdvService;
import org.apollo.protos.Protocol.ReasonCode;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.Protocol.Inventory.InventoryType;
import org.apollo.protos.Protocol.Transaction.Contract.ContractType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "net")
@Component
public class TransactionsMsgHandler implements TronMsgHandler {

  private static int MAX_TRX_SIZE = 50_000;
  private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;
  @Autowired
  private ApolloNetDelegate apolloNetDelegate;
  @Autowired
  private AdvService advService;

  private BlockingQueue<TrxEvent> smartContractQueue = new LinkedBlockingQueue(MAX_TRX_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private int threadNum = Args.getInstance().getValidateSignThreadNum();
  private ExecutorService trxHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L,
      TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors
      .newSingleThreadScheduledExecutor();

  public void init() {
    handleSmartContract();
  }

  public void close() {
    smartContractExecutor.shutdown();
  }

  public boolean isBusy() {
    return queue.size() + smartContractQueue.size() > MAX_TRX_SIZE;
  }

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {
    TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
    check(peer, transactionsMessage);
    for (Transaction trx : transactionsMessage.getTransactions().getTransactionsList()) {
      int type = trx.getRawData().getContract(0).getType().getNumber();
      if (type == ContractType.TriggerSmartContract_VALUE
          || type == ContractType.CreateSmartContract_VALUE) {
        if (!smartContractQueue.offer(new TrxEvent(peer, new TransactionMessage(trx)))) {
          logger.warn("Add smart contract failed, queueSize {}:{}", smartContractQueue.size(),
              queue.size());
        }
      } else {
        trxHandlePool.submit(() -> handleTransaction(peer, new TransactionMessage(trx)));
      }
    }
  }

  private void check(PeerConnection peer, TransactionsMessage msg) throws P2pException {
    for (Transaction trx : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(trx).getMessageId(), InventoryType.TRX);
      if (!peer.getAdvInvRequest().containsKey(item)) {
        throw new P2pException(TypeEnum.BAD_MESSAGE,
            "trx: " + msg.getMessageId() + " without request.");
      }
      peer.getAdvInvRequest().remove(item);
    }
  }

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
          TrxEvent event = smartContractQueue.take();
          trxHandlePool.submit(() -> handleTransaction(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        logger.error("Handle smart contract exception.", e);
      }
    }, 1000, 20, TimeUnit.MILLISECONDS);
  }

  private void handleTransaction(PeerConnection peer, TransactionMessage trx) {
    if (peer.isDisconnect()) {
      logger.warn("Drop trx {} from {}, peer is disconnect.", trx.getMessageId(),
          peer.getInetAddress());
      return;
    }

    if (advService.getMessage(new Item(trx.getMessageId(), InventoryType.TRX)) != null) {
      return;
    }

    try {
      apolloNetDelegate.pushTransaction(trx.getTransactionCapsule());
      advService.broadcast(trx);
    } catch (P2pException e) {
      logger.warn("Trx {} from peer {} process failed. type: {}, reason: {}",
          trx.getMessageId(), peer.getInetAddress(), e.getType(), e.getMessage());
      if (e.getType().equals(TypeEnum.BAD_TRX)) {
        peer.disconnect(ReasonCode.BAD_TX);
      }
    } catch (Exception e) {
      logger.error("Trx {} from peer {} process failed.", trx.getMessageId(), peer.getInetAddress(),
          e);
    }
  }

  class TrxEvent {

    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public TrxEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}