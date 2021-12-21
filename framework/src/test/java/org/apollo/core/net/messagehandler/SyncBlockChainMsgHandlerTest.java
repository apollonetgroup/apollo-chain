package org.apollo.core.net.messagehandler;

import java.util.ArrayList;

import org.apollo.core.exception.P2pException;
import org.apollo.core.net.message.SyncBlockChainMessage;
import org.apollo.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.apollo.core.net.peer.PeerConnection;
import org.junit.Assert;
import org.junit.Test;

public class SyncBlockChainMsgHandlerTest {

  private SyncBlockChainMsgHandler handler = new SyncBlockChainMsgHandler();
  private PeerConnection peer = new PeerConnection();

  @Test
  public void testProcessMessage() {
    try {
      handler.processMessage(peer, new SyncBlockChainMessage(new ArrayList<>()));
    } catch (P2pException e) {
      Assert.assertTrue(e.getMessage().equals("SyncBlockChain blockIds is empty"));
    }
  }

}
