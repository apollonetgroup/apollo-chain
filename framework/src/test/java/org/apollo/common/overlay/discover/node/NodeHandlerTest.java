package org.apollo.common.overlay.discover.node;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.overlay.discover.node.Node;
import org.apollo.common.overlay.discover.node.NodeHandler;
import org.apollo.common.overlay.discover.node.NodeManager;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.ChainBaseManager;
import org.apollo.core.Constant;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NodeHandlerTest {

  private static final Logger logger = LoggerFactory.getLogger("Test");
  private Manager dbManager;
  private ApolloApplicationContext context;
  private Application appTest;
  private CommonParameter argsTest;
  private Node currNode;
  private Node oldNode;
  private Node replaceNode;
  private NodeHandler currHandler;
  private NodeHandler oldHandler;
  private NodeHandler replaceHandler;
  private NodeManager nodeManager;

  /**
   * init the application.
   */
  @Before
  public void init() {
    argsTest = Args.getInstance();
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.initServices(argsTest);
    appTest.startServices();
    appTest.startup();
  }

  /**
   * destroy the context.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File("output-directory"))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * init nodes.
   */
  @Before
  public void initNodes() {
    dbManager = context.getBean(Manager.class);
    nodeManager = new NodeManager(context.getBean(ChainBaseManager.class));
    String currNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537"
        + "898407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    String oldNodeId = "74c11ffad1d59d7b2c56691a0b84a53f0791c92361357364f1d2537898407e"
        + "f0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    String replaceNodeId = "74c11ffad1d59d7b1a56691a0b84a53f0791c92361357364f1d2537"
        + "837407ef0249bbbf5a4ce8cff9e34e2fdf8bac883540e026d1e5d6ebf536414bdde81198e";
    currNode = new Node(currNodeId.getBytes(), "47.95.206.44", 18885, 18888);
    oldNode = new Node(oldNodeId.getBytes(), "36.95.165.44", 18885, 18888);
    replaceNode = new Node(replaceNodeId.getBytes(), "47.29.177.44", 18885, 18888);
    currHandler = new NodeHandler(currNode, nodeManager);
    oldHandler = new NodeHandler(oldNode, nodeManager);
    replaceHandler = new NodeHandler(replaceNode, nodeManager);
  }

  @Test
  public void stateNonActiveTest() throws Exception {
    Class clazz = NodeHandler.class;
    Constructor<NodeHandler> cn = clazz.getDeclaredConstructor(Node.class, NodeManager.class);
    NodeHandler nh = cn.newInstance(oldNode, nodeManager);
    Field declaredField = clazz.getDeclaredField("replaceCandidate");
    declaredField.setAccessible(true);
    declaredField.set(nh, replaceHandler);

    nodeManager.getTable().addNode(oldNode);
    nh.changeState(NodeHandler.State.EVICTCANDIDATE);
    nh.changeState(NodeHandler.State.NONACTIVE);
    replaceHandler.changeState(NodeHandler.State.ALIVE);

    Assert.assertTrue(!nodeManager.getTable().contains(oldNode));
    Assert.assertTrue(nodeManager.getTable().contains(replaceNode));
  }
}
