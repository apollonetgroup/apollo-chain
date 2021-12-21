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

package org.apollo.common.overlay.discover.dht;

import java.math.BigInteger;

import org.apollo.common.utils.Utils;
import org.apollo.core.Constant;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

public class Peer {

  private byte[] id;
  private String host = Constant.LOCAL_HOST;
  private int port = 0;

  public Peer(byte[] id, String host, int port) {
    this.id = id;
    this.host = host;
    this.port = port;
  }

  public Peer(byte[] ip) {
    this.id = ip;
  }

  public static byte[] randomPeerId() {

    byte[] peerIdBytes = new BigInteger(512, Utils.getRandom()).toByteArray();

    final String peerId;
    if (peerIdBytes.length > 64) {
      peerId = Hex.toHexString(peerIdBytes, 1, 64);
    } else {
      peerId = Hex.toHexString(peerIdBytes);
    }

    return Hex.decode(peerId);
  }

  public byte nextBit(String startPattern) {

    if (this.toBinaryString().startsWith(startPattern + "1")) {
      return 1;
    } else {
      return 0;
    }
  }

  public byte[] calcDistance(Peer toPeer) {

    BigInteger aaPeer = new BigInteger(getId());
    BigInteger bbPeer = new BigInteger(toPeer.getId());

    BigInteger distance = aaPeer.xor(bbPeer);
    return BigIntegers.asUnsignedByteArray(distance);
  }

  public byte[] getId() {
    return id;
  }

  public void setId(byte[] id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return String
        .format("Peer {\n id=%s, \n host=%s, \n port=%d\n}", Hex.toHexString(id), host, port);
  }

  public String toBinaryString() {

    BigInteger bi = new BigInteger(1, id);
    String out = String.format("%512s", bi.toString(2));
    out = out.replace(' ', '0');

    return out;
  }

}
