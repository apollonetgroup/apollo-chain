/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.apollo.core.vm.program;


import org.apollo.common.runtime.vm.DataWord;
import org.apollo.core.capsule.*;
import org.apollo.core.store.*;
import org.apollo.core.vm.program.invoke.ProgramInvoke;
import org.apollo.core.vm.program.listener.ProgramListener;
import org.apollo.core.vm.program.listener.ProgramListenerAware;
import org.apollo.core.vm.repository.Key;
import org.apollo.core.vm.repository.Repository;
import org.apollo.core.vm.repository.Value;
import org.apollo.protos.Protocol.AccountType;

public class ContractState implements Repository, ProgramListenerAware {

  // contract address
  private final DataWord address;
  private Repository repository;
  private ProgramListener programListener;

  ContractState(ProgramInvoke programInvoke) {
    this.address = programInvoke.getContractAddress();
    this.repository = programInvoke.getDeposit();
  }

  @Override
  public void setProgramListener(ProgramListener listener) {
    this.programListener = listener;
  }

  @Override
  public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
    return repository.getAssetIssue(tokenId);
  }

  @Override
  public AssetIssueV2Store getAssetIssueV2Store() {
    return repository.getAssetIssueV2Store();
  }

  @Override
  public AssetIssueStore getAssetIssueStore() {
    return repository.getAssetIssueStore();
  }

  @Override
  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return repository.getDynamicPropertiesStore();
  }

  @Override
  public AccountCapsule createAccount(byte[] addr, AccountType type) {
    return repository.createAccount(addr, type);
  }

  @Override
  public AccountCapsule createAccount(byte[] address, String accountName, AccountType type) {
    return repository.createAccount(address, accountName, type);
  }


  @Override
  public AccountCapsule getAccount(byte[] addr) {
    return repository.getAccount(addr);
  }

  public BytesCapsule getDynamicProperty(byte[] bytesKey) {
    return repository.getDynamicProperty(bytesKey);
  }

  @Override
  public DelegatedResourceCapsule getDelegatedResource(byte[] key) {
    return repository.getDelegatedResource(key);
  }

  @Override
  public WitnessCapsule getWitness(byte[] address) {
    return repository.getWitness(address);
  }

  @Override
  public void deleteContract(byte[] address) {
    repository.deleteContract(address);
  }

  @Override
  public void createContract(byte[] codeHash, ContractCapsule contractCapsule) {
    repository.createContract(codeHash, contractCapsule);
  }

  @Override
  public ContractCapsule getContract(byte[] codeHash) {
    return repository.getContract(codeHash);
  }

  @Override
  public void updateContract(byte[] address, ContractCapsule contractCapsule) {
    repository.updateContract(address, contractCapsule);
  }

  @Override
  public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
    repository.updateAccount(address, accountCapsule);
  }

  @Override
  public void saveCode(byte[] address, byte[] code) {
    repository.saveCode(address, code);
  }

  @Override
  public byte[] getCode(byte[] address) {
    return repository.getCode(address);
  }

  @Override
  public void putStorageValue(byte[] addr, DataWord key, DataWord value) {
    if (canListenTrace(addr)) {
      programListener.onStoragePut(key, value);
    }
    repository.putStorageValue(addr, key, value);
  }

  private boolean canListenTrace(byte[] address) {
    return (programListener != null) && this.address.equals(new DataWord(address));
  }

  @Override
  public DataWord getStorageValue(byte[] addr, DataWord key) {
    return repository.getStorageValue(addr, key);
  }

  @Override
  public long getBalance(byte[] addr) {
    return repository.getBalance(addr);
  }

  @Override
  public long addBalance(byte[] addr, long value) {
    return repository.addBalance(addr, value);
  }

  @Override
  public Repository newRepositoryChild() {
    return repository.newRepositoryChild();
  }

  @Override
  public void setParent(Repository repository) {
    this.repository.setParent(repository);
  }

  @Override
  public void commit() {
    repository.commit();
  }

  @Override
  public void putAccount(Key key, Value value) {
    repository.putAccount(key, value);
  }

  @Override
  public void putCode(Key key, Value value) {
    repository.putCode(key, value);
  }


  @Override
  public void putContract(Key key, Value value) {
    repository.putContract(key, value);
  }


  public void putStorage(Key key, Storage cache) {
    repository.putStorage(key, cache);
  }


  @Override
  public Storage getStorage(byte[] address) {
    return repository.getStorage(address);
  }

  @Override
  public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
    this.repository.putAccountValue(address, accountCapsule);
  }

  @Override
  public void putDelegatedResource(Key key, Value value) {
    repository.putDelegatedResource(key, value);
  }

  @Override
  public void putDelegation(Key key, Value value) {
    repository.putDelegation(key, value);
  }

  @Override
  public long addTokenBalance(byte[] address, byte[] tokenId, long value) {
    return repository.addTokenBalance(address, tokenId, value);
  }

  @Override
  public long getTokenBalance(byte[] address, byte[] tokenId) {
    return repository.getTokenBalance(address, tokenId);
  }

  @Override
  public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {
    return repository.getAccountLeftEnergyFromFreeze(accountCapsule);
  }

  @Override
  public long calculateGlobalEnergyLimit(AccountCapsule accountCapsule) {
    return repository.calculateGlobalEnergyLimit(accountCapsule);
  }

  @Override
  public byte[] getBlackHoleAddress() {
    return repository.getBlackHoleAddress();
  }

  @Override
  public BlockCapsule getBlockByNum(long num) {
    return repository.getBlockByNum(num);
  }

  @Override
  public AccountCapsule createNormalAccount(byte[] address) {
    return repository.createNormalAccount(address);
  }

  @Override
  public DelegationStore getDelegationStore() {
    return repository.getDelegationStore();
  }

  @Override
  public VotesCapsule getVotes(byte[] address) {
    return repository.getVotes(address);
  }

  @Override
  public long getBeginCycle(byte[] address) {
    return repository.getBeginCycle(address);
  }

  @Override
  public long getEndCycle(byte[] address) {
    return repository.getEndCycle(address);
  }

  @Override
  public AccountCapsule getAccountVote(long cycle, byte[] address) {
    return repository.getAccountVote(cycle, address);
  }

  @Override
  public BytesCapsule getDelegation(Key key) {
    return repository.getDelegation(key);
  }

  @Override
  public void updateDynamicProperty(byte[] word, BytesCapsule bytesCapsule) {
    repository.updateDynamicProperty(word, bytesCapsule);
  }

  @Override
  public void updateDelegatedResource(byte[] word, DelegatedResourceCapsule delegatedResourceCapsule) {
    repository.updateDelegatedResource(word, delegatedResourceCapsule);
  }

  @Override
  public void updateVotes(byte[] word, VotesCapsule votesCapsule) {
    repository.updateVotes(word, votesCapsule);
  }

  @Override
  public void updateBeginCycle(byte[] word, long cycle) {
    repository.updateBeginCycle(word, cycle);
  }

  @Override
  public void updateEndCycle(byte[] word, long cycle) {
    repository.updateEndCycle(word, cycle);
  }

  @Override
  public void updateAccountVote(byte[] word, long cycle, AccountCapsule accountCapsule) {
    repository.updateAccountVote(word, cycle, accountCapsule);
  }

  @Override
  public void updateDelegation(byte[] word, BytesCapsule bytesCapsule) {
    repository.updateDelegation(word, bytesCapsule);
  }

  @Override
  public void putDynamicProperty(Key key, Value value) {
    repository.putDynamicProperty(key, value);
  }

  @Override
  public void putVotes(Key key, Value value) {
    repository.putVotes(key, value);
  }

  @Override
  public void addTotalNetWeight(long amount) {
    repository.addTotalNetWeight(amount);
  }

  @Override
  public void addTotalEnergyWeight(long amount) {
    repository.addTotalEnergyWeight(amount);
  }

  @Override
  public void saveTotalNetWeight(long totalNetWeight) {
    repository.saveTotalNetWeight(totalNetWeight);
  }

  @Override
  public void saveTotalEnergyWeight(long totalEnergyWeight) {
    repository.saveTotalEnergyWeight(totalEnergyWeight);
  }

  @Override
  public long getTotalNetWeight() {
    return repository.getTotalNetWeight();
  }

  @Override
  public long getTotalEnergyWeight() {
    return repository.getTotalEnergyWeight();
  }

}
