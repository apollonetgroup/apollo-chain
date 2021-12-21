package org.apollo.core.db;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.utils.ByteUtil;
import org.apollo.core.capsule.PbftSignCapsule;
import org.apollo.protos.Protocol.PBFTMessage.DataType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PbftSignDataStore extends TronDatabase<PbftSignCapsule> {

  public PbftSignDataStore() {
    super("pbft-sign-data");
  }

  @Override
  public void put(byte[] key, PbftSignCapsule item) {
    dbSource.putData(key, item.getData());
  }

  @Override
  public PbftSignCapsule get(byte[] key) {
    byte[] data = dbSource.getData(key);
    if (ByteUtil.isNullOrZeroArray(data)) {
      return null;
    }
    return new PbftSignCapsule(data);
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public void putSrSignData(long epoch, PbftSignCapsule item) {
    put(buildSrSignKey(epoch), item);
  }

  public PbftSignCapsule getSrSignData(long epoch) {
    return get(buildSrSignKey(epoch));
  }

  public void putBlockSignData(long blockNum, PbftSignCapsule item) {
    put(buildBlockSignKey(blockNum), item);
  }

  public PbftSignCapsule getBlockSignData(long blockNum) {
    return get(buildBlockSignKey(blockNum));
  }

  private byte[] buildSrSignKey(long epoch) {
    return (DataType.SRL.toString() + epoch).getBytes();
  }

  private byte[] buildBlockSignKey(long blockNum) {
    return (DataType.BLOCK.toString() + blockNum).getBytes();
  }
}