package org.apollo.core.net.messagehandler;

import java.util.ArrayList;

import org.apollo.core.net.message.InventoryMessage;
import org.apollo.core.net.messagehandler.InventoryMsgHandler;
import org.apollo.core.net.peer.PeerConnection;
import org.apollo.protos.Protocol.Inventory.InventoryType;
import org.junit.Test;

public class InventoryMsgHandlerTest {

  private InventoryMsgHandler handler = new InventoryMsgHandler();
  private PeerConnection peer = new PeerConnection();

  @Test
  public void testProcessMessage() {
    InventoryMessage msg = new InventoryMessage(new ArrayList<>(), InventoryType.TRX);

    peer.setNeedSyncFromPeer(true);
    peer.setNeedSyncFromUs(true);
    handler.processMessage(peer, msg);

    peer.setNeedSyncFromPeer(true);
    peer.setNeedSyncFromUs(false);
    handler.processMessage(peer, msg);

    peer.setNeedSyncFromPeer(false);
    peer.setNeedSyncFromUs(true);
    handler.processMessage(peer, msg);

  }
}
