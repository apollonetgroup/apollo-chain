package org.apollo.core.vm.repository;

import org.apollo.common.runtime.vm.DataWord;
import org.apollo.core.capsule.*;
import org.apollo.core.store.*;
import org.apollo.core.vm.program.Storage;
import org.apollo.protos.Protocol;

public interface Repository {

  AssetIssueCapsule getAssetIssue(byte[] tokenId);

  AssetIssueV2Store getAssetIssueV2Store();

  AssetIssueStore getAssetIssueStore();

  DynamicPropertiesStore getDynamicPropertiesStore();

  DelegationStore getDelegationStore();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

  BytesCapsule getDynamicProperty(byte[] bytesKey);

  DelegatedResourceCapsule getDelegatedResource(byte[] key);

  VotesCapsule getVotes(byte[] address);

  long getBeginCycle(byte[] address);

  long getEndCycle(byte[] address);

  AccountCapsule getAccountVote(long cycle, byte[] address);

  BytesCapsule getDelegation(Key key);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractCapsule contractCapsule);

  ContractCapsule getContract(byte[] address);

  void updateContract(byte[] address, ContractCapsule contractCapsule);

  void updateAccount(byte[] address, AccountCapsule accountCapsule);

  void updateDynamicProperty(byte[] word, BytesCapsule bytesCapsule);

  void updateDelegatedResource(byte[] word, DelegatedResourceCapsule delegatedResourceCapsule);

  void updateVotes(byte[] word, VotesCapsule votesCapsule);

  void updateBeginCycle(byte[] word, long cycle);

  void updateEndCycle(byte[] word, long cycle);

  void updateAccountVote(byte[] word, long cycle, AccountCapsule accountCapsule);

  void updateDelegation(byte[] word, BytesCapsule bytesCapsule);

  void saveCode(byte[] address, byte[] code);

  byte[] getCode(byte[] address);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);

  Repository newRepositoryChild();

  void setParent(Repository deposit);

  void commit();

  void putAccount(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putAccountValue(byte[] address, AccountCapsule accountCapsule);

  void putDynamicProperty(Key key, Value value);

  void putDelegatedResource(Key key, Value value);

  void putVotes(Key key, Value value);

  void putDelegation(Key key, Value value);

  long addTokenBalance(byte[] address, byte[] tokenId, long value);

  long getTokenBalance(byte[] address, byte[] tokenId);

  long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule);

  long calculateGlobalEnergyLimit(AccountCapsule accountCapsule);

  byte[] getBlackHoleAddress();

  BlockCapsule getBlockByNum(final long num);

  AccountCapsule createNormalAccount(byte[] address);

  WitnessCapsule getWitness(byte[] address);

  void addTotalNetWeight(long amount);

  void addTotalEnergyWeight(long amount);

  void saveTotalNetWeight(long totalNetWeight);

  void saveTotalEnergyWeight(long totalEnergyWeight);

  long getTotalNetWeight();

  long getTotalEnergyWeight();

}
