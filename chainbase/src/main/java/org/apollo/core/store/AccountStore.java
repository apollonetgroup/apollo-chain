package org.apollo.core.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.Commons;
import org.apollo.core.capsule.AccountAssetCapsule;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.capsule.utils.AssetUtil;
import org.apollo.core.config.DefaultConstants;
import org.apollo.core.db.TronStoreWithRevoking;
import org.apollo.core.db.accountstate.AccountStateCallBackUtils;
import org.apollo.protos.Protocol.Account;
import org.apollo.protos.Protocol.AccountAsset;
import org.apollo.protos.contract.BalanceContract.TransactionBalanceTrace;
import org.apollo.protos.contract.BalanceContract.TransactionBalanceTrace.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigObject;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStateCallBackUtils accountStateCallBackUtils;

  @Autowired
  private BalanceTraceStore balanceTraceStore;

  @Autowired
  private AccountTraceStore accountTraceStore;

  @Autowired
  private AccountAssetStore accountAssetStore;

  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    if (CommonParameter.getInstance().isHistoryBalanceLookup()) {
      AccountCapsule old = super.getUnchecked(key);
      if (old == null) {
        if (item.getBalance() != 0) {
          recordBalance(item, item.getBalance());
          BlockCapsule.BlockId blockId = balanceTraceStore.getCurrentBlockId();
          if (blockId != null) {
            accountTraceStore.recordBalanceWithBlock(key, blockId.getNum(), item.getBalance());
          }
        }
      } else if (old.getBalance() != item.getBalance()) {
        recordBalance(item, item.getBalance() - old.getBalance());
        BlockCapsule.BlockId blockId = balanceTraceStore.getCurrentBlockId();
        if (blockId != null) {
          accountTraceStore.recordBalanceWithBlock(key, blockId.getNum(), item.getBalance());
        }
      }
    }

    if (AssetUtil.isAllowAssetOptimization()) {
      Account account = item.getInstance();
      AccountAsset accountAsset = AssetUtil.getAsset(account);
      if (null != accountAsset) {
        accountAssetStore.put(key, new AccountAssetCapsule(
                accountAsset));
        account = AssetUtil.clearAsset(account);
        item.setIsAssetImport(false);
        item.setInstance(account);
      }
    }
    super.put(key, item);
    accountStateCallBackUtils.accountCallBack(key, item);
  }

  @Override
  public void delete(byte[] key) {
    if (CommonParameter.getInstance().isHistoryBalanceLookup()) {
      AccountCapsule old = super.getUnchecked(key);
      if (old != null) {
        recordBalance(old, -old.getBalance());
      }

      BlockCapsule.BlockId blockId = balanceTraceStore.getCurrentBlockId();
      if (blockId != null) {
        accountTraceStore.recordBalanceWithBlock(key, blockId.getNum(), 0);
      }
    }

    super.delete(key);

    if (AssetUtil.isAllowAssetOptimization()) {
      accountAssetStore.delete(key);
    }
  }

  /**
   * Max TRX account.
   */
  public AccountCapsule getSun() {
    return getUnchecked(assertsAddress.get("Sun"));
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get(DefaultConstants.BLACK_HOLE_NAME));
  }


  public byte[] getBlackholeAddress() {
    return assertsAddress.get(DefaultConstants.BLACK_HOLE_NAME);
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getFounder() {
    return getUnchecked(assertsAddress.get(DefaultConstants.FOUNDER_ADDR));
  }
  
  public AccountCapsule getGrant() {
    return getUnchecked(assertsAddress.get(DefaultConstants.DELEGATION_GRANT_NAME));
  }


  // do somethings
  // check old balance and new balance, if equals, do nothing, then get balance trace from balancetraceStore
  private void recordBalance(AccountCapsule accountCapsule, long diff) {
    TransactionBalanceTrace transactionBalanceTrace = balanceTraceStore.getCurrentTransactionBalanceTrace();

    if (transactionBalanceTrace == null) {
      return;
    }

    long operationIdentifier;
    OptionalLong max = transactionBalanceTrace.getOperationList().stream()
        .mapToLong(Operation::getOperationIdentifier)
        .max();
    if (max.isPresent()) {
      operationIdentifier = max.getAsLong() + 1;
    } else {
      operationIdentifier = 0;
    }

    ByteString address = accountCapsule.getAddress();
    Operation operation = Operation.newBuilder()
        .setAddress(address)
        .setAmount(diff)
        .setOperationIdentifier(operationIdentifier)
        .build();
    transactionBalanceTrace = transactionBalanceTrace.toBuilder()
        .addOperation(operation)
        .build();
    balanceTraceStore.setCurrentTransactionBalanceTrace(transactionBalanceTrace);
  }

  @Override
  public void close() {
    super.close();
  }
}
