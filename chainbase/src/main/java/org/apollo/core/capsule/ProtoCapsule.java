package org.apollo.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
