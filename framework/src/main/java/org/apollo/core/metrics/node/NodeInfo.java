package org.apollo.core.metrics.node;

import lombok.Data;

@Data
public class NodeInfo {
  private String ip;
  private int nodeType;
  private String version;
  private int backupStatus;
}
