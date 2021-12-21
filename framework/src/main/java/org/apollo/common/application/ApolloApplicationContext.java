package org.apollo.common.application;

import org.apollo.common.overlay.discover.DiscoverServer;
import org.apollo.common.overlay.discover.node.NodeManager;
import org.apollo.common.overlay.server.ChannelManager;
import org.apollo.core.db.Manager;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApolloApplicationContext extends AnnotationConfigApplicationContext {

  public ApolloApplicationContext() {
  }

  public ApolloApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public ApolloApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public ApolloApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    super.destroy();
  }
}
