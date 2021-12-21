package org.apollo.core.exception;

public class TooBigTransactionResultException extends ApolloException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
