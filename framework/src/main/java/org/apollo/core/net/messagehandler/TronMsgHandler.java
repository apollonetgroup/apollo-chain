package org.apollo.core.net.messagehandler;

import org.apollo.core.exception.P2pException;
import org.apollo.core.net.message.TronMessage;
import org.apollo.core.net.peer.PeerConnection;

public interface TronMsgHandler {

  void processMessage(PeerConnection peer, TronMessage msg) throws P2pException;

}
