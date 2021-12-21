package org.apollo.common.runtime.vm;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.crypto.Hash;
import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.runtime.vm.DataWord;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.ByteUtil;
import org.apollo.common.utils.Sha256Hash;
import org.apollo.common.utils.StringUtil;
import org.apollo.core.Constant;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.store.StoreFactory;
import org.apollo.core.vm.PrecompiledContracts.ValidateMultiSign;
import org.apollo.core.vm.repository.Repository;
import org.apollo.core.vm.repository.RepositoryImpl;
import org.apollo.protos.Protocol;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.utils.AbiUtil;

@Slf4j
public class ValidateMultiSignContractTest {

  private static final String dbPath = "output_PrecompiledContracts_test";
  private static final String METHOD_SIGN = "validatemultisign(address,uint256,bytes32,bytes[])";
  private static final byte[] longData;
  private static ApolloApplicationContext context;
  private static Application appT;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveTotalSignNum(5);

    longData = new byte[100000000];
    Arrays.fill(longData, (byte) 2);
  }

  ValidateMultiSign contract = new ValidateMultiSign();

  @Test
  void testAddressNonExist() {
    byte[] hash = Hash.sha3(longData);
    ECKey key = new ECKey();
    byte[] sign = key.sign(hash).toByteArray();
    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(sign));

    //Address non exist
    Assert.assertEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), 1, hash, signs)
            .getValue(), DataWord.ZERO().getData());
  }

  @Test
  void testDifferentCase() {
    //Create an account with permission

    ECKey key = new ECKey();
    AccountCapsule toAccount = new AccountCapsule(ByteString.copyFrom(key.getAddress()),
        Protocol.AccountType.Normal,
        System.currentTimeMillis(), true, dbManager.getDynamicPropertiesStore());

    ECKey key1 = new ECKey();
    ECKey key2 = new ECKey();

    Protocol.Permission activePermission =
        Protocol.Permission.newBuilder()
            .setType(Protocol.Permission.PermissionType.Active)
            .setId(2)
            .setPermissionName("active")
            .setThreshold(2)
            .setOperations(ByteString.copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
            .addKeys(Protocol.Key.newBuilder().setAddress(ByteString.copyFrom(key1.getAddress()))
                .setWeight(1).build())
            .addKeys(
                Protocol.Key.newBuilder()
                    .setAddress(ByteString.copyFrom(key2.getAddress()))
                    .setWeight(1)
                    .build())
            .build();

    toAccount
        .updatePermissions(toAccount.getPermissionById(0), null, Arrays.asList(activePermission));
    dbManager.getAccountStore().put(key.getAddress(), toAccount);

    //generate data

    byte[] address = key.getAddress();
    int permissionId = 2;
    byte[] data = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), longData);

    //combine data
    byte[] merged = ByteUtil.merge(address, ByteArray.fromInt(permissionId), data);
    //sha256 of it
    byte[] toSign = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), merged);

    //sign data

    List<Object> signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    //add Repetitive
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    signs.add(Hex.toHexString(key2.sign(toSign).toByteArray()));

    Assert.assertEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ONE().getData());

    //weight not enough
    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    Assert.assertEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());

    //put wrong sign
    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    Assert.assertEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());

    signs = new ArrayList<>();
    signs.add(Hex.toHexString(key1.sign(toSign).toByteArray()));
    signs.add(Hex.toHexString(new ECKey().sign(toSign).toByteArray()));

    Assert.assertEquals(
        validateMultiSign(StringUtil.encode58Check(key.getAddress()), permissionId, data, signs)
            .getValue(), DataWord.ZERO().getData());
  }


  Pair<Boolean, byte[]> validateMultiSign(String address, int permissionId, byte[] hash,
      List<Object> signatures) {
    List<Object> parameters = Arrays
        .asList(address, permissionId, "0x" + Hex.toHexString(hash), signatures);
    byte[] input = Hex.decode(AbiUtil.parseParameters(METHOD_SIGN, parameters));
    Repository deposit = RepositoryImpl.createRoot(StoreFactory.getInstance());
    logger.info("energy for data:{}", contract.getEnergyForData(input));
    contract.setRepository(deposit);

    Pair<Boolean, byte[]> ret = contract.execute(input);

    logger.info("BytesArray:{}，HexString:{}", Arrays.toString(ret.getValue()),
        Hex.toHexString(ret.getValue()));
    return ret;
  }


}
