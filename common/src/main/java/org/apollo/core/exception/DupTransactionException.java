package org.apollo.core.exception;

public class DupTransactionException extends ApolloException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
