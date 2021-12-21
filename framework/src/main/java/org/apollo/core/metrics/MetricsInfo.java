package org.apollo.core.metrics;

import lombok.Data;

import org.apollo.core.metrics.blockchain.BlockChainInfo;
import org.apollo.core.metrics.net.NetInfo;
import org.apollo.core.metrics.node.NodeInfo;

@Data
public class MetricsInfo {
  private long interval;
  private NodeInfo node;
  private BlockChainInfo blockchain;
  private NetInfo net;
}
