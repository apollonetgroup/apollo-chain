package org.apollo.core.db;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.capsule.BytesCapsule;
import org.apollo.core.db.TronStoreWithRevoking;
import org.apollo.core.db2.common.TxCacheDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class TransactionCache extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public TransactionCache(@Value("trans-cache") String dbName) {
    super(new TxCacheDB(dbName));
  }
}
