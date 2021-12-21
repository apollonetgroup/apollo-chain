/*
 * java-apollo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-apollo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apollo.core.capsule.utils;

import com.google.protobuf.ByteString;

import org.apollo.protos.Protocol.TXInput;

public class TxInputUtil {

  /**
   * new transaction input.
   *
   * @param txId byte[] txId
   * @param vout int vout
   * @param signature byte[] signature
   * @param pubKey byte[] pubKey
   * @return {@link TXInput}
   */
  public static TXInput newTxInput(byte[] txId, long vout, byte[]
      signature, byte[] pubKey) {

    TXInput.raw.Builder rawBuilder = TXInput.raw.newBuilder();

    TXInput.raw rawData = rawBuilder
        .setTxID(ByteString.copyFrom(txId))
        .setVout(vout)
        .setPubKey(ByteString.copyFrom(pubKey)).build();

    return TXInput.newBuilder()
        .setSignature(ByteString.copyFrom(signature))
        .setRawData(rawData).build();
  }
}
