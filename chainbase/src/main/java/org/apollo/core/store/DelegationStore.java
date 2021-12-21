package org.apollo.core.store;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.capsule.BytesCapsule;
import org.apollo.core.config.DefaultConstants;
import org.apollo.core.db.TronStoreWithRevoking;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Slf4j
@Component
public class DelegationStore extends TronStoreWithRevoking<BytesCapsule> {

  public static final long REMARK = -1L;
  public static final int DEFAULT_BROKERAGE = 1;
  public static final BigInteger DECIMAL_OF_VI_REWARD = DefaultConstants.DECIMAL_OF_VI_REWARD;

  @Autowired
  public DelegationStore(@Value("delegation") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public void addReward(long cycle, byte[] address, long value) {
    byte[] key = buildRewardKey(cycle, address);
    BytesCapsule bytesCapsule = get(key);
    if (bytesCapsule == null) {
      put(key, new BytesCapsule(ByteArray.fromLong(value)));
    } else {
      put(key, new BytesCapsule(ByteArray
          .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
    }
  }

  public long getReward(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildRewardKey(cycle, address));
    if (bytesCapsule == null) {
      return 0L;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public void setBeginCycle(byte[] address, long number) {
    put(address, new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getBeginCycle(byte[] address) {
    BytesCapsule bytesCapsule = get(address);
    return bytesCapsule == null ? 0 : ByteArray.toLong(bytesCapsule.getData());
  }

  public void setEndCycle(byte[] address, long number) {
    put(buildEndCycleKey(address), new BytesCapsule(ByteArray.fromLong(number)));
  }

  public long getEndCycle(byte[] address) {
    BytesCapsule bytesCapsule = get(buildEndCycleKey(address));
    return bytesCapsule == null ? REMARK : ByteArray.toLong(bytesCapsule.getData());
  }

  public void setWitnessVote(long cycle, byte[] address, long value) {
    put(buildVoteKey(cycle, address), new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getWitnessVote(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildVoteKey(cycle, address));
    if (bytesCapsule == null) {
      return REMARK;
    } else {
      return ByteArray.toLong(bytesCapsule.getData());
    }
  }

  public void setAccountVote(long cycle, byte[] address, AccountCapsule accountCapsule) {
    put(buildAccountVoteKey(cycle, address), new BytesCapsule(accountCapsule.getData()));
  }

  public AccountCapsule getAccountVote(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildAccountVoteKey(cycle, address));
    if (bytesCapsule == null) {
      return null;
    } else {
      return new AccountCapsule(bytesCapsule.getData());
    }
  }

  public void setBrokerage(long cycle, byte[] address, int brokerage) {
    put(buildBrokerageKey(cycle, address), new BytesCapsule(ByteArray.fromInt(brokerage)));
  }

  public int getBrokerage(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildBrokerageKey(cycle, address));
    if (bytesCapsule == null) {
      return DEFAULT_BROKERAGE;
    } else {
      return ByteArray.toInt(bytesCapsule.getData());
    }
  }

  public void setBrokerage(byte[] address, int brokerage) {
    setBrokerage(-1, address, brokerage);
  }

  public int getBrokerage(byte[] address) {
    return getBrokerage(-1, address);
  }

  public void setWitnessVi(long cycle, byte[] address, BigInteger value) {
    put(buildViKey(cycle, address), new BytesCapsule(value.toByteArray()));
  }

  public BigInteger getWitnessVi(long cycle, byte[] address) {
    BytesCapsule bytesCapsule = get(buildViKey(cycle, address));
    if (bytesCapsule == null) {
      return BigInteger.ZERO;
    } else {
      return new BigInteger(bytesCapsule.getData());
    }
  }

  public void accumulateWitnessVi(long cycle, byte[] address, long voteCount) {
    BigInteger preVi = getWitnessVi(cycle - 1, address);
    long reward = getReward(cycle, address);
    if (reward == 0 || voteCount == 0) { // Just forward pre vi
      if (!BigInteger.ZERO.equals(preVi)) { // Zero vi will not be record
        setWitnessVi(cycle, address, preVi);
      }
    } else { // Accumulate delta vi
      BigInteger deltaVi = BigInteger.valueOf(reward)
          .multiply(DECIMAL_OF_VI_REWARD)
          .divide(BigInteger.valueOf(voteCount));
      setWitnessVi(cycle, address, preVi.add(deltaVi));
    }
  }

  private byte[] buildVoteKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-vote").getBytes();
  }

  private byte[] buildRewardKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-reward").getBytes();
  }

  private byte[] buildAccountVoteKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-account-vote").getBytes();
  }

  private byte[] buildEndCycleKey(byte[] address) {
    return ("end-" + Hex.toHexString(address)).getBytes();
  }

  private byte[] buildBrokerageKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-brokerage").getBytes();
  }

  private byte[] buildViKey(long cycle, byte[] address) {
    return (cycle + "-" + Hex.toHexString(address) + "-vi").getBytes();
  }

}
