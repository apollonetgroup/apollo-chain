package org.apollo.core.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.apollo.common.zksnark.JLibrustzcash;
import org.apollo.core.Constant;
import org.apollo.core.exception.ZksnarkException;
import org.apollo.keystore.Wallet;

@AllArgsConstructor
public class DiversifierT {

  @Setter
  @Getter
  private byte[] data = new byte[Constant.ZC_DIVERSIFIER_SIZE];

  public DiversifierT() {
  }

  public static DiversifierT random() throws ZksnarkException {
    byte[] d;
    while (true) {
      d = Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (JLibrustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }
    return new DiversifierT(d);
  }
}
