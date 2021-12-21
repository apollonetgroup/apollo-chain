package org.apollo.consensus.base;

import org.apollo.consensus.pbft.message.PbftBaseMessage;
import org.apollo.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}