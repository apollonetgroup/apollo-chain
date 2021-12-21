package org.apollo.core.store;

import com.google.common.collect.Streams;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.capsule.AbiCapsule;
import org.apollo.core.db.TronStoreWithRevoking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j(topic = "DB")
@Component
public class AbiStore extends TronStoreWithRevoking<AbiCapsule> {

  @Autowired
  private AbiStore(@Value("abi") String dbName) {
    super(dbName);
  }

  @Override
  public AbiCapsule get(byte[] key) {
    return getUnchecked(key);
  }

  public void put(byte[] key, byte[] value) {
    if (Objects.isNull(key) || Objects.isNull(value)) {
      return;
    }

    revokingDB.put(key, value);
  }

  public long getTotalABIs() {
    return Streams.stream(revokingDB.iterator()).count();
  }
}
