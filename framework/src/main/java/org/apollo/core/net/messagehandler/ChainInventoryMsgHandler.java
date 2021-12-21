package org.apollo.core.net.messagehandler;

import static org.apollo.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apollo.core.capsule.BlockCapsule.BlockId;
import org.apollo.core.config.Parameter.ChainConstant;
import org.apollo.core.config.Parameter.NetConstants;
import org.apollo.core.exception.P2pException;
import org.apollo.core.exception.P2pException.TypeEnum;
import org.apollo.core.net.ApolloNetDelegate;
import org.apollo.core.net.message.ChainInventoryMessage;
import org.apollo.core.net.message.TronMessage;
import org.apollo.core.net.peer.PeerConnection;
import org.apollo.core.net.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "net")
@Component
public class ChainInventoryMsgHandler implements TronMsgHandler {

  @Autowired
  private ApolloNetDelegate apolloNetDelegate;

  @Autowired
  private SyncService syncService;

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {

    ChainInventoryMessage chainInventoryMessage = (ChainInventoryMessage) msg;

    check(peer, chainInventoryMessage);

    peer.setNeedSyncFromPeer(true);

    peer.setSyncChainRequested(null);

    Deque<BlockId> blockIdWeGet = new LinkedList<>(chainInventoryMessage.getBlockIds());

    if (blockIdWeGet.size() == 1 && apolloNetDelegate.containBlock(blockIdWeGet.peek())) {
      peer.setNeedSyncFromPeer(false);
      return;
    }

    while (!peer.getSyncBlockToFetch().isEmpty()) {
      if (peer.getSyncBlockToFetch().peekLast().equals(blockIdWeGet.peekFirst())) {
        break;
      }
      peer.getSyncBlockToFetch().pollLast();
    }

    blockIdWeGet.poll();

    peer.setRemainNum(chainInventoryMessage.getRemainNum());
    peer.getSyncBlockToFetch().addAll(blockIdWeGet);

    synchronized (apolloNetDelegate.getBlockLock()) {
      while (!peer.getSyncBlockToFetch().isEmpty() && apolloNetDelegate
          .containBlock(peer.getSyncBlockToFetch().peek())) {
        BlockId blockId = peer.getSyncBlockToFetch().pop();
        peer.setBlockBothHave(blockId);
        logger.info("Block {} from {} is processed", blockId.getString(), peer.getNode().getHost());
      }
    }

    if ((chainInventoryMessage.getRemainNum() == 0 && !peer.getSyncBlockToFetch().isEmpty())
        || (chainInventoryMessage.getRemainNum() != 0
        && peer.getSyncBlockToFetch().size() > NetConstants.SYNC_FETCH_BATCH_NUM)) {
      syncService.setFetchFlag(true);
    } else {
      syncService.syncNext(peer);
    }
  }

  private void check(PeerConnection peer, ChainInventoryMessage msg) throws P2pException {
    if (peer.getSyncChainRequested() == null) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "not send syncBlockChainMsg");
    }

    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "blockIds is empty");
    }

    if (blockIds.size() > NetConstants.SYNC_FETCH_BATCH_NUM + 1) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "big blockIds size: " + blockIds.size());
    }

    if (msg.getRemainNum() != 0 && blockIds.size() < NetConstants.SYNC_FETCH_BATCH_NUM) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "remain: " + msg.getRemainNum() + ", blockIds size: " + blockIds.size());
    }

    long num = blockIds.get(0).getNum();
    for (BlockId id : msg.getBlockIds()) {
      if (id.getNum() != num++) {
        throw new P2pException(TypeEnum.BAD_MESSAGE, "not continuous block");
      }
    }

    if (!peer.getSyncChainRequested().getKey().contains(blockIds.get(0))) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "unlinked block, my head: "
          + peer.getSyncChainRequested().getKey().getLast().getString()
          + ", peer: " + blockIds.get(0).getString());
    }

    if (apolloNetDelegate.getHeadBlockId().getNum() > 0) {
      long maxRemainTime =
          ChainConstant.CLOCK_MAX_DELAY + System.currentTimeMillis() - apolloNetDelegate
              .getBlockTime(apolloNetDelegate.getSolidBlockId());
      long maxFutureNum =
          maxRemainTime / BLOCK_PRODUCED_INTERVAL + apolloNetDelegate.getSolidBlockId().getNum();
      long lastNum = blockIds.get(blockIds.size() - 1).getNum();
      if (lastNum + msg.getRemainNum() > maxFutureNum) {
        throw new P2pException(TypeEnum.BAD_MESSAGE, "lastNum: " + lastNum + " + remainNum: "
            + msg.getRemainNum() + " > futureMaxNum: " + maxFutureNum);
      }
    }
  }

}
