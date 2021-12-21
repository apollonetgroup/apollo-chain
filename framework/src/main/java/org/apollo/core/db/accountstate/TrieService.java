package org.apollo.core.db.accountstate;

import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;

import java.util.Arrays;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apollo.common.crypto.Hash;
import org.apollo.core.ChainBaseManager;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.db.accountstate.storetrie.AccountStateStoreTrie;
import org.springframework.stereotype.Component;

@Slf4j(topic = "AccountState")
@Component
public class TrieService {

  @Setter
  private ChainBaseManager chainBaseManager;

  @Setter
  private AccountStateStoreTrie accountStateStoreTrie;

  public byte[] getFullAccountStateRootHash() {
    long latestNumber = chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    return getAccountStateRootHash(latestNumber);
  }

  public byte[] getSolidityAccountStateRootHash() {
    long latestSolidityNumber =
        chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum();
    return getAccountStateRootHash(latestSolidityNumber);
  }

  private byte[] getAccountStateRootHash(long blockNumber) {
    long latestNumber = blockNumber;
    byte[] rootHash = null;
    try {
      BlockCapsule blockCapsule = chainBaseManager.getBlockByNum(latestNumber);
      ByteString value = blockCapsule.getInstance().getBlockHeader().getRawData()
          .getAccountStateRoot();
      rootHash = value == null ? null : value.toByteArray();
      if (Arrays.equals(rootHash, Internal.EMPTY_BYTE_ARRAY)) {
        rootHash = Hash.EMPTY_TRIE_HASH;
      }
    } catch (Exception e) {
      logger.error("Get the {} block error.", latestNumber, e);
    }
    return rootHash;
  }
}
