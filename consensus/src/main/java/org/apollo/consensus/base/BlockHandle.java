package org.apollo.consensus.base;

import org.apollo.consensus.base.Param.Miner;
import org.apollo.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}