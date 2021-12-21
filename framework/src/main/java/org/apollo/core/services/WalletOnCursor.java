package org.apollo.core.services;

import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.db.Manager;
import org.apollo.core.db2.core.Chainbase;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j(topic = "API")
public abstract class WalletOnCursor {

  protected Chainbase.Cursor cursor = Chainbase.Cursor.HEAD;
  @Autowired
  private Manager dbManager;

  public <T> T futureGet(TronCallable<T> callable) {
    try {
      dbManager.setCursor(cursor);
      return callable.call();
    } finally {
      dbManager.resetCursor();
    }
  }

  public void futureGet(Runnable runnable) {
    try {
      dbManager.setCursor(cursor);
      runnable.run();
    } finally {
      dbManager.resetCursor();
    }
  }

  public interface TronCallable<T> extends Callable<T> {

    @Override
    T call();
  }
}
