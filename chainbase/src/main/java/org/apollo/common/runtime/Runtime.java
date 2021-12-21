package org.apollo.common.runtime;

import org.apollo.core.db.TransactionContext;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}
