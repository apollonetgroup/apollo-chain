package org.apollo.core.net.messagehandler;

import static org.apollo.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.apollo.core.config.Parameter.ChainConstant.BLOCK_SIZE;
import lombok.extern.slf4j.Slf4j;

import org.apollo.core.Constant;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.capsule.BlockCapsule.BlockId;
import org.apollo.core.config.args.Args;
import org.apollo.core.exception.P2pException;
import org.apollo.core.exception.P2pException.TypeEnum;
import org.apollo.core.net.ApolloNetDelegate;
import org.apollo.core.net.message.BlockMessage;
import org.apollo.core.net.message.TronMessage;
import org.apollo.core.net.peer.Item;
import org.apollo.core.net.peer.PeerConnection;
import org.apollo.core.net.service.AdvService;
import org.apollo.core.net.service.SyncService;
import org.apollo.core.services.WitnessProductBlockService;
import org.apollo.protos.Protocol.Inventory.InventoryType;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "net")
@Component
public class BlockMsgHandler implements TronMsgHandler {

  @Autowired
  private ApolloNetDelegate apolloNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  private int maxBlockSize = BLOCK_SIZE + Constant.ONE_THOUSAND;

  private boolean fastForward = Args.getInstance().isFastForward();

  @Override
  public void processMessage(PeerConnection peer, TronMessage msg) throws P2pException {

    BlockMessage blockMessage = (BlockMessage) msg;
    BlockId blockId = blockMessage.getBlockId();

    if (!fastForward && !peer.isFastForwardPeer()) {
      check(peer, blockMessage);
    }

    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      syncService.processBlock(peer, blockMessage);
    } else {
      Long time = peer.getAdvInvRequest().remove(new Item(blockId, InventoryType.BLOCK));
      long now = System.currentTimeMillis();
      long interval = blockId.getNum() - apolloNetDelegate.getHeadBlockId().getNum();
      processBlock(peer, blockMessage.getBlockCapsule());
      logger.info(
          "Receive block/interval {}/{} from {} fetch/delay {}/{}ms, "
              + "txs/process {}/{}ms, witness: {}",
          blockId.getNum(),
          interval,
          peer.getInetAddress(),
          time == null ? 0 : now - time,
          now - blockMessage.getBlockCapsule().getTimeStamp(),
          ((BlockMessage) msg).getBlockCapsule().getTransactions().size(),
          System.currentTimeMillis() - now,
          Hex.toHexString(blockMessage.getBlockCapsule().getWitnessAddress().toByteArray()));
    }
  }

  private void check(PeerConnection peer, BlockMessage msg) throws P2pException {
    Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
    if (!peer.getSyncBlockRequested().containsKey(msg.getBlockId()) && !peer.getAdvInvRequest()
        .containsKey(item)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "no request");
    }
    BlockCapsule blockCapsule = msg.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > maxBlockSize) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
    }
  }

  private void processBlock(PeerConnection peer, BlockCapsule block) throws P2pException {
    BlockId blockId = block.getBlockId();
    if (!apolloNetDelegate.containBlock(block.getParentBlockId())) {
      logger.warn("Get unlink block {} from {}, head is {}.", blockId.getString(),
          peer.getInetAddress(), apolloNetDelegate.getHeadBlockId().getString());
      syncService.startSync(peer);
      return;
    }

    Item item = new Item(blockId, InventoryType.BLOCK);
    if (fastForward || peer.isFastForwardPeer()) {
      peer.getAdvInvReceive().put(item, System.currentTimeMillis());
      advService.addInvToCache(item);
    }

    if (fastForward) {
      if (block.getNum() < apolloNetDelegate.getHeadBlockId().getNum()) {
        logger.warn("Receive a low block {}, head {}",
            blockId.getString(), apolloNetDelegate.getHeadBlockId().getString());
        return;
      }
      if (apolloNetDelegate.validBlock(block)) {
        advService.fastForward(new BlockMessage(block));
        apolloNetDelegate.trustNode(peer);
      }
    }

    apolloNetDelegate.processBlock(block, false);
    witnessProductBlockService.validWitnessProductTwoBlock(block);
    apolloNetDelegate.getActivePeer().forEach(p -> {
      if (p.getAdvInvReceive().getIfPresent(blockId) != null) {
        p.setBlockBothHave(blockId);
      }
    });

    if (!fastForward) {
      advService.broadcast(new BlockMessage(block));
    }
  }

}
