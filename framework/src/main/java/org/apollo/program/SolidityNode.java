package org.apollo.program;

import static org.apollo.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.overlay.client.DatabaseGrpcClient;
import org.apollo.common.overlay.discover.DiscoverServer;
import org.apollo.common.overlay.discover.node.NodeManager;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.core.ChainBaseManager;
import org.apollo.core.Constant;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.net.ApolloNetService;
import org.apollo.core.services.RpcApiService;
import org.apollo.core.services.http.solidity.SolidityNodeHttpApiService;
import org.apollo.protos.Protocol.Block;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

@Slf4j(topic = "app")
public class SolidityNode {

  private Manager dbManager;

  private ChainBaseManager chainBaseManager;

  private DatabaseGrpcClient databaseGrpcClient;

  private AtomicLong ID = new AtomicLong();

  private AtomicLong remoteBlockNum = new AtomicLong();

  private LinkedBlockingDeque<Block> blockQueue = new LinkedBlockingDeque(100);

  private int exceptionSleepTime = 1000;

  private volatile boolean flag = true;

  public SolidityNode(Manager dbManager) {
    this.dbManager = dbManager;
    this.chainBaseManager = dbManager.getChainBaseManager();
    resolveCompatibilityIssueIfUsingFullNodeDatabase();
    ID.set(chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
    databaseGrpcClient = new DatabaseGrpcClient(Args.getInstance().getTrustNodeAddr());
    remoteBlockNum.set(getLastSolidityBlockNum());
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) {
    logger.info("Solidity node is running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = Args.getInstance();

    logger.info("index switch is {}",
        BooleanUtils.toStringOnOff(BooleanUtils
            .toBoolean(parameter.getStorage().getIndexSwitch())));

    if (StringUtils.isEmpty(parameter.getTrustNodeAddr())) {
      logger.error("Trust node is not set.");
      return;
    }
    parameter.setSolidityNode(true);

    ApplicationContext context = new ApolloApplicationContext(DefaultConfig.class);

    if (parameter.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    FullNode.shutdown(appT);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    //http
    SolidityNodeHttpApiService httpApiService = context.getBean(SolidityNodeHttpApiService.class);
    if (CommonParameter.getInstance().solidityNodeHttpEnable) {
      appT.addService(httpApiService);
    }

    appT.initServices(parameter);
    appT.startServices();
    appT.startup();

    //Disable peer discovery for solidity node
    DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
    discoverServer.close();
    NodeManager nodeManager = context.getBean(NodeManager.class);
    nodeManager.close();
    ApolloNetService apolloNetService = context.getBean(ApolloNetService.class);
    apolloNetService.stop();

    SolidityNode node = new SolidityNode(appT.getDbManager());
    node.start();

    rpcApiService.blockUntilShutdown();
  }

  private void start() {
    try {
      new Thread(() -> getBlock()).start();
      new Thread(() -> processBlock()).start();
      logger.info("Success to start solid node, ID: {}, remoteBlockNum: {}.", ID.get(),
          remoteBlockNum);
    } catch (Exception e) {
      logger
          .error("Failed to start solid node, address: {}.", Args.getInstance().getTrustNodeAddr());
      System.exit(0);
    }
  }

  private void getBlock() {
    long blockNum = ID.incrementAndGet();
    while (flag) {
      try {
        if (blockNum > remoteBlockNum.get()) {
          sleep(BLOCK_PRODUCED_INTERVAL);
          remoteBlockNum.set(getLastSolidityBlockNum());
          continue;
        }
        Block block = getBlockByNum(blockNum);
        blockQueue.put(block);
        blockNum = ID.incrementAndGet();
      } catch (Exception e) {
        logger.error("Failed to get block {}, reason: {}.", blockNum, e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private void processBlock() {
    while (flag) {
      try {
        Block block = blockQueue.take();
        loopProcessBlock(block);
      } catch (Exception e) {
        logger.error(e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private void loopProcessBlock(Block block) {
    while (flag) {
      long blockNum = block.getBlockHeader().getRawData().getNumber();
      try {
        dbManager.pushVerifiedBlock(new BlockCapsule(block));
        chainBaseManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(blockNum);
        logger
            .info("Success to process block: {}, blockQueueSize: {}.", blockNum, blockQueue.size());
        return;
      } catch (Exception e) {
        logger.error("Failed to process block {}.", new BlockCapsule(block), e);
        sleep(exceptionSleepTime);
        block = getBlockByNum(blockNum);
      }
    }
  }

  private Block getBlockByNum(long blockNum) {
    while (true) {
      try {
        long time = System.currentTimeMillis();
        Block block = databaseGrpcClient.getBlock(blockNum);
        long num = block.getBlockHeader().getRawData().getNumber();
        if (num == blockNum) {
          logger.info("Success to get block: {}, cost: {}ms.",
              blockNum, System.currentTimeMillis() - time);
          return block;
        } else {
          logger.warn("Get block id not the same , {}, {}.", num, blockNum);
          sleep(exceptionSleepTime);
        }
      } catch (Exception e) {
        logger.error("Failed to get block: {}, reason: {}.", blockNum, e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  private long getLastSolidityBlockNum() {
    while (true) {
      try {
        long time = System.currentTimeMillis();
        long blockNum = databaseGrpcClient.getDynamicProperties().getLastSolidityBlockNum();
        logger.info("Get last remote solid blockNum: {}, remoteBlockNum: {}, cost: {}.",
            blockNum, remoteBlockNum, System.currentTimeMillis() - time);
        return blockNum;
      } catch (Exception e) {
        logger.error("Failed to get last solid blockNum: {}, reason: {}.", remoteBlockNum.get(),
            e.getMessage());
        sleep(exceptionSleepTime);
      }
    }
  }

  public void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (Exception e1) {
      logger.error(e1.getMessage());
    }
  }

  private void resolveCompatibilityIssueIfUsingFullNodeDatabase() {
    long lastSolidityBlockNum =
        chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    long headBlockNum = chainBaseManager.getHeadBlockNum();
    logger.info("headBlockNum:{}, solidityBlockNum:{}, diff:{}",
        headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
    if (lastSolidityBlockNum < headBlockNum) {
      logger.info("use fullNode database, headBlockNum:{}, solidityBlockNum:{}, diff:{}",
          headBlockNum, lastSolidityBlockNum, headBlockNum - lastSolidityBlockNum);
      chainBaseManager.getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(headBlockNum);
    }
  }
}