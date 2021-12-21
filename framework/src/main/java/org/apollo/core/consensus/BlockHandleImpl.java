package org.apollo.core.consensus;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.backup.BackupManager;
import org.apollo.common.backup.BackupManager.BackupStatusEnum;
import org.apollo.consensus.Consensus;
import org.apollo.consensus.base.BlockHandle;
import org.apollo.consensus.base.State;
import org.apollo.consensus.base.Param.Miner;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.db.Manager;
import org.apollo.core.net.ApolloNetService;
import org.apollo.core.net.message.BlockMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private ApolloNetService apolloNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    if (blockCapsule == null) {
      return null;
    }
    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      apolloNetService.fastForward(blockMessage);
      manager.pushBlock(blockCapsule);
      apolloNetService.broadcast(blockMessage);
    } catch (Exception e) {
      logger.error("Handle block {} failed.", blockCapsule.getBlockId().getString(), e);
      return null;
    }
    return blockCapsule;
  }
}
