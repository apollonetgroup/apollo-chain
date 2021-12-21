package org.apollo.core.exception;

public class ApolloException extends Exception {

  public ApolloException() {
    super();
  }

  public ApolloException(String message) {
    super(message);
  }

  public ApolloException(String message, Throwable cause) {
    super(message, cause);
  }

}
