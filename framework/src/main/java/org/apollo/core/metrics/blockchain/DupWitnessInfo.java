package org.apollo.core.metrics.blockchain;

import lombok.Data;

@Data
public class DupWitnessInfo {
  private String address;
  private long blockNum;
  private int count;
}
