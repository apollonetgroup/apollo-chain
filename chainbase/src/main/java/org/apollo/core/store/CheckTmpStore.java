package org.apollo.core.store;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Spliterator;
import java.util.function.Consumer;

import org.apollo.core.db.TronDatabase;
import org.apollo.core.exception.BadItemException;
import org.apollo.core.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CheckTmpStore extends TronDatabase<byte[]> {

  @Autowired
  public CheckTmpStore(ApplicationContext ctx) {
    super("tmp");
  }

  @Override
  public void put(byte[] key, byte[] item) {
  }

  @Override
  public void delete(byte[] key) {

  }

  @Override
  public byte[] get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  @Override
  public void forEach(Consumer action) {

  }

  @Override
  public Spliterator spliterator() {
    return null;
  }
}