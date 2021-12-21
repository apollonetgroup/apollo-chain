package org.apollo.tool.litefullnode.db;

import java.io.Closeable;
import java.io.IOException;

import org.apollo.tool.litefullnode.iterator.DBIterator;

public interface DBInterface extends Closeable {

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void delete(byte[] key);

  DBIterator iterator();

  long size();

  void close() throws IOException;

}
