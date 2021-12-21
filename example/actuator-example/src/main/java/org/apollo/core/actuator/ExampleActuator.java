package org.apollo.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.actuator.AbstractActuator;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;

@Slf4j(topic = "actuator")
public class ExampleActuator extends AbstractActuator {

  public ExampleActuator() {
    super(null, null);
    logger.info("ExampleActuator construct successfully.");
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    logger.info("ExampleActuator execute successfully.");
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    logger.info("ExampleActuator validate successfully.");
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return null;
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
