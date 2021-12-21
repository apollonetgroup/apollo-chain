package org.apollo.core.services.interfaceOnPBFT;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.db2.core.Chainbase;
import org.apollo.core.services.WalletOnCursor;
import org.springframework.stereotype.Component;

@Slf4j(topic = "API")
@Component
public class WalletOnPBFT extends WalletOnCursor {

  public WalletOnPBFT() {
    super.cursor = Chainbase.Cursor.PBFT;
  }
}
