package org.apollo.consensus.dpos;

import static org.apollo.core.config.DefaultConstants.BLOCK_PRODUCE_CYCLE_MILLISECOND;

import com.google.protobuf.ByteString;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.consensus.ConsensusDelegate;
import org.apollo.consensus.base.State;
import org.apollo.consensus.base.Param.Miner;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.protos.Protocol.BlockHeader;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "consensus")
@Component
public class DposTask {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposSlot dposSlot;

  @Autowired
  private StateManager stateManager;

  @Setter
  private DposService dposService;

  private Thread produceThread;

  private volatile boolean isRunning = true;

  public void init() {

    if (!dposService.isEnable() || StringUtils.isEmpty(dposService.getMiners())) {
      return;
    }

    Runnable runnable = () -> {
      while (isRunning) {
        try {
          if (dposService.isNeedSyncCheck()) {
            Thread.sleep(1000);
            dposService.setNeedSyncCheck(dposSlot.getTime(1) < System.currentTimeMillis());
          } else {
            long time =
            		BLOCK_PRODUCE_CYCLE_MILLISECOND - System.currentTimeMillis() % BLOCK_PRODUCE_CYCLE_MILLISECOND;
            Thread.sleep(time);
            State state = produceBlock();
            if (!State.OK.equals(state)) {
              logger.info("Produce block failed: {}", state);
            }
          }
        } catch (Throwable throwable) {
          logger.error("Produce block error.", throwable);
        }
      }
    };
    produceThread = new Thread(runnable, "DPosMiner");
    produceThread.start();
    logger.info("DPoS task started.");
  }

  public void stop() {
    isRunning = false;
    if (produceThread != null) {
      produceThread.interrupt();
    }
    logger.info("DPoS task stopped.");
  }

  private State produceBlock() {

    State state = stateManager.getState();
    if (!State.OK.equals(state)) {
      return state;
    }

    synchronized (dposService.getBlockHandle().getLock()) {

      long slot = dposSlot.getSlot(System.currentTimeMillis() + 50);
      if (slot == 0) {
        return State.NOT_TIME_YET;
      }

      ByteString pWitness = dposSlot.getScheduledWitness(slot);

      Miner miner = dposService.getMiners().get(pWitness);
      if (miner == null) {
        return State.NOT_MY_TURN;
      }

      long pTime = dposSlot.getTime(slot);
      long timeout =
          pTime + BLOCK_PRODUCE_CYCLE_MILLISECOND / 2 * dposService.getBlockProduceTimeoutPercent() / 100;
      BlockCapsule blockCapsule = dposService.getBlockHandle().produce(miner, pTime, timeout);
      if (blockCapsule == null) {
        return State.PRODUCE_BLOCK_FAILED;
      }

      BlockHeader.raw raw = blockCapsule.getInstance().getBlockHeader().getRawData();
      logger.info("Produce block successfully, num: {}, time: {}, witness: {}, ID:{}, parentID:{}",
          raw.getNumber(),
          new DateTime(raw.getTimestamp()),
          ByteArray.toHexString(raw.getWitnessAddress().toByteArray()),
          new Sha256Hash(raw.getNumber(), Sha256Hash.of(CommonParameter
              .getInstance().isECKeyCryptoEngine(), raw.toByteArray())),
          ByteArray.toHexString(raw.getParentHash().toByteArray()));
    }

    return State.OK;
  }

}
