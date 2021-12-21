package org.apollo.common.runtime;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apollo.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.apollo.common.logsfilter.trigger.ContractTrigger;
import org.apollo.common.runtime.vm.DataWord;
import org.apollo.common.runtime.vm.LogInfo;
import org.apollo.common.utils.ByteArraySet;
import org.apollo.core.capsule.TransactionResultCapsule;
import org.apollo.protos.Protocol.Transaction.Result.contractResult;

public class ProgramResult {

  private long energyUsed = 0;
  private long futureRefund = 0;

  private byte[] hReturn = EMPTY_BYTE_ARRAY;
  private byte[] contractAddress = EMPTY_BYTE_ARRAY;
  private RuntimeException exception;
  private boolean revert;

  private Set<DataWord> deleteAccounts;
  private ByteArraySet touchedAccounts = new ByteArraySet();
  private List<InternalTransaction> internalTransactions;
  private List<LogInfo> logInfoList;
  private TransactionResultCapsule ret = new TransactionResultCapsule();

  @Setter
  private List<ContractTrigger> triggerList;

  @Setter
  @Getter
  private String runtimeError;

  @Getter
  @Setter
  private contractResult resultCode;

  /*
   * for testing runs ,
   * call/create is not executed
   * but dummy recorded
   */
  private List<CallCreate> callCreateList;

  public static ProgramResult createEmpty() {
    ProgramResult result = new ProgramResult();
    result.setHReturn(EMPTY_BYTE_ARRAY);
    return result;
  }

  public void spendEnergy(long energy) {
    energyUsed += energy;
  }

  public void setRevert() {
    this.revert = true;
  }

  public boolean isRevert() {
    return revert;
  }

  public void refundEnergy(long energy) {
    energyUsed -= energy;
  }

  public byte[] getContractAddress() {
    return Arrays.copyOf(contractAddress, contractAddress.length);
  }

  public void setContractAddress(byte[] contractAddress) {
    this.contractAddress = Arrays.copyOf(contractAddress, contractAddress.length);
  }

  public byte[] getHReturn() {
    return hReturn;
  }

  public void setHReturn(byte[] hReturn) {
    this.hReturn = hReturn;

  }

  public List<ContractTrigger> getTriggerList() {
    return triggerList != null ? triggerList : new LinkedList<>();
  }

  public TransactionResultCapsule getRet() {
    return ret;
  }

  public void setRet(TransactionResultCapsule ret) {
    this.ret = ret;
  }

  public RuntimeException getException() {
    return exception;
  }

  public void setException(RuntimeException exception) {
    this.exception = exception;
  }

  public long getEnergyUsed() {
    return energyUsed;
  }

  public Set<DataWord> getDeleteAccounts() {
    if (deleteAccounts == null) {
      deleteAccounts = new HashSet<>();
    }
    return deleteAccounts;
  }

  public void addDeleteAccount(DataWord address) {
    getDeleteAccounts().add(address);
  }

  public void addDeleteAccounts(Set<DataWord> accounts) {
    if (!isEmpty(accounts)) {
      getDeleteAccounts().addAll(accounts);
    }
  }

  public void addTouchAccount(byte[] addr) {
    touchedAccounts.add(addr);
  }

  public Set<byte[]> getTouchedAccounts() {
    return touchedAccounts;
  }

  public void addTouchAccounts(Set<byte[]> accounts) {
    if (!isEmpty(accounts)) {
      getTouchedAccounts().addAll(accounts);
    }
  }

  public List<LogInfo> getLogInfoList() {
    if (logInfoList == null) {
      logInfoList = new ArrayList<>();
    }
    return logInfoList;
  }

  public void addLogInfo(LogInfo logInfo) {
    getLogInfoList().add(logInfo);
  }

  public void addLogInfos(List<LogInfo> logInfos) {
    if (!isEmpty(logInfos)) {
      getLogInfoList().addAll(logInfos);
    }
  }

  public List<CallCreate> getCallCreateList() {
    if (callCreateList == null) {
      callCreateList = new ArrayList<>();
    }
    return callCreateList;
  }

  public void addCallCreate(byte[] data, byte[] destination, byte[] energyLimit, byte[] value) {
    getCallCreateList().add(new CallCreate(data, destination, energyLimit, value));
  }

  public List<InternalTransaction> getInternalTransactions() {
    if (internalTransactions == null) {
      internalTransactions = new ArrayList<>();
    }
    return internalTransactions;
  }

  public InternalTransaction addInternalTransaction(byte[] parentHash, int deep,
      byte[] senderAddress, byte[] transferAddress, long value, byte[] data, String note,
      long nonce, Map<String, Long> token) {
    InternalTransaction transaction = new InternalTransaction(parentHash, deep,
        size(internalTransactions), senderAddress, transferAddress, value, data, note, nonce,
        token);
    getInternalTransactions().add(transaction);
    return transaction;
  }

  public void addInternalTransaction(InternalTransaction internalTransaction) {
    getInternalTransactions().add(internalTransaction);
  }

  public void addInternalTransactions(List<InternalTransaction> internalTransactions) {
    getInternalTransactions().addAll(internalTransactions);
  }

  public void rejectInternalTransactions() {
    for (InternalTransaction internalTx : getInternalTransactions()) {
      internalTx.reject();
    }
  }

  public void addFutureRefund(long energyValue) {
    futureRefund += energyValue;
  }

  public long getFutureRefund() {
    return futureRefund;
  }

  public void resetFutureRefund() {
    futureRefund = 0;
  }

  public void reset() {
    getDeleteAccounts().clear();
    getLogInfoList().clear();
    resetFutureRefund();
  }

  public void merge(ProgramResult another) {
    addInternalTransactions(another.getInternalTransactions());
    if (another.getException() == null && !another.isRevert()) {
      addDeleteAccounts(another.getDeleteAccounts());
      addLogInfos(another.getLogInfoList());
      addFutureRefund(another.getFutureRefund());
      addTouchAccounts(another.getTouchedAccounts());
    }
  }

}
