package org.apollo.common.runtime.vm;

import com.google.protobuf.ByteString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.application.Application;
import org.apollo.common.application.ApplicationFactory;
import org.apollo.common.application.ApolloApplicationContext;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.runtime.Runtime;
import org.apollo.common.runtime.TvmTestUtils;
import org.apollo.common.storage.DepositImpl;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.FileUtil;
import org.apollo.common.utils.StringUtil;
import org.apollo.common.utils.Utils;
import org.apollo.core.Constant;
import org.apollo.core.Wallet;
import org.apollo.core.capsule.AccountCapsule;
import org.apollo.core.config.DefaultConfig;
import org.apollo.core.config.args.Args;
import org.apollo.core.db.Manager;
import org.apollo.core.exception.ContractExeException;
import org.apollo.core.exception.ContractValidateException;
import org.apollo.core.exception.ReceiptCheckErrException;
import org.apollo.core.exception.VMIllegalException;
import org.apollo.protos.Protocol.AccountType;
import org.apollo.protos.Protocol.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import stest.apollo.wallet.common.client.utils.AbiUtil;

@Slf4j
public class BatchSendTest {

  private static final String dbPath = "output_BatchSendTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;
  private static final long TOTAL_SUPPLY = 1000_000_000L;
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://apollo.network";
  private static Runtime runtime;
  private static Manager dbManager;
  private static ApolloApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static AccountCapsule ownerCapsule;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new ApolloApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TRANSFER_TO = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
    deposit.addBalance(Hex.decode(TRANSFER_TO), 10);
    deposit.commit();
    ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            AccountType.AssetIssue);

    ownerCapsule.setBalance(1000_1000_1000L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);

  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * pragma solidity ^0.5.4;
   *
   * contract TestBatchSendTo { constructor() public payable{}
   *
   * function depositIn() public payable{}
   *
   *
   * function batchSendTo (address  payable to1 ,address  payable to2 ,address  payable to3, uint256
   * m1,uint256 m2,uint256 m3) public { to1.send(m1 ); to2.send(m2 ); to3.send(m3 ); }
   *
   * }
   */
  @Test
  public void TransferTokenTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    //  1. Deploy*/
    byte[] contractAddress = deployTransferContract();
    deposit.commit();
    Assert.assertEquals(1000,
        dbManager.getAccountStore().get(contractAddress).getBalance());

    String selectorStr = "batchSendTo(address,address,address,uint256,uint256,uint256)";
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    ECKey ecKey3 = new ECKey(Utils.getRandom());

    List<Object> params = new ArrayList<>();
    params.add(StringUtil.encode58Check(ecKey1.getAddress()));
    params.add(StringUtil.encode58Check(ecKey2.getAddress()));
    params.add(StringUtil.encode58Check(ecKey3.getAddress()));
    params.add(100);
    params.add(1100);
    params.add(200);
    byte[] input = Hex.decode(AbiUtil
        .parseMethod(selectorStr, params));

    //  2. Test trigger with tokenValue and tokenId, also test internal transaction
    // transferToken function */
    long triggerCallValue = 0;
    long feeLimit = 100000000;
    long tokenValue = 0;
    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), contractAddress,
            input,
            triggerCallValue, feeLimit, tokenValue, 0);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, dbManager, null);
    Assert.assertNull(runtime.getRuntimeError());
    //send success, create account
    Assert.assertEquals(100,
        dbManager.getAccountStore().get(ecKey1.getAddress()).getBalance());
    //send failed, do not create account
    Assert.assertNull(dbManager.getAccountStore().get(ecKey2.getAddress()));
    //send success, create account
    Assert.assertEquals(200,
        dbManager.getAccountStore().get(ecKey3.getAddress()).getBalance());

  }

  private byte[] deployTransferContract()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "TestTransferTo";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[]";
    String code = "608060405261019c806100136000396000f3fe608060405260043610610045577c0100000000000"
        + "00000000000000000000000000000000000000000000060003504632a205edf811461004a5780634cd2270c"
        + "146100c8575b600080fd5b34801561005657600080fd5b50d3801561006357600080fd5b50d2801561007057"
        + "600080fd5b506100c6600480360360c081101561008757600080fd5b5073ffffffffffffffffffffffffffff"
        + "ffffffffffff813581169160208101358216916040820135169060608101359060808101359060a001356100"
        + "d0565b005b6100c661016e565b60405173ffffffffffffffffffffffffffffffffffffffff87169084156108"
        + "fc029085906000818181858888f1505060405173ffffffffffffffffffffffffffffffffffffffff89169350"
        + "85156108fc0292508591506000818181858888f1505060405173ffffffffffffffffffffffffffffffffffff"
        + "ffff8816935084156108fc0292508491506000818181858888f15050505050505050505050565b56fea16562"
        + "7a7a72305820cc2d598d1b3f968bbdc7825ce83d22dad48192f4bf95bda7f9e4ddf61669ba830029";

    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long tokenValue = 0;
    long tokenId = 0;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, tokenValue, tokenId,
            deposit, null);
    return contractAddress;
  }
}
