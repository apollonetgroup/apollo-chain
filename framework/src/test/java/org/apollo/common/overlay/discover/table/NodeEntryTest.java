package org.apollo.common.overlay.discover.table;

import org.apollo.common.overlay.discover.node.Node;
import org.apollo.common.overlay.discover.table.NodeEntry;
import org.junit.Assert;
import org.junit.Test;

public class NodeEntryTest {

  @Test
  public void test() throws InterruptedException {
    Node node1 = Node.instanceOf("127.0.0.1:10001");
    NodeEntry nodeEntry = new NodeEntry(node1);
    int distance = nodeEntry.getDistance();
    Assert.assertEquals(-256, distance);

    long lastModified = nodeEntry.getModified();
    //System.out.println(lastModified);
    Thread.sleep(1);
    nodeEntry.touch();
    long nowModified = nodeEntry.getModified();
    //System.out.println(nowModified);
    Assert.assertNotEquals(lastModified, nowModified);

    Node node2 = Node.instanceOf("127.0.0.1:10002");
    NodeEntry nodeEntry2 = new NodeEntry(node2);
    boolean isDif = nodeEntry.equals(nodeEntry2);
    Assert.assertTrue(isDif);
  }

}
