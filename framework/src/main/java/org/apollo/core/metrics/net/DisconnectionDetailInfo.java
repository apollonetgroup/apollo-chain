package org.apollo.core.metrics.net;

import lombok.Data;

@Data
public class DisconnectionDetailInfo {
  private String reason;
  private int count;
}
