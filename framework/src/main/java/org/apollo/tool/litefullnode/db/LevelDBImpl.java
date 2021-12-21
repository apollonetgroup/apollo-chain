package org.apollo.tool.litefullnode.db;

import com.google.common.collect.Streams;

import java.io.IOException;

import org.apollo.tool.litefullnode.iterator.DBIterator;
import org.apollo.tool.litefullnode.iterator.LevelDBIterator;
import org.iq80.leveldb.DB;

public class LevelDBImpl implements DBInterface {

  private DB leveldb;

  public LevelDBImpl(DB leveldb) {
    this.leveldb = leveldb;
  }

  @Override
  public byte[] get(byte[] key) {
    return leveldb.get(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    leveldb.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    leveldb.delete(key);
  }

  @Override
  public DBIterator iterator() {
    return new LevelDBIterator(leveldb.iterator());
  }

  @Override
  public long size() {
    return Streams.stream(leveldb.iterator()).count();
  }

  @Override
  public void close() throws IOException {
    leveldb.close();
  }
}
