package org.apollo.core.actuator;

import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}