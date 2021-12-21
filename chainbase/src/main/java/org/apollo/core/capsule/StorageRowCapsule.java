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

package org.apollo.core.capsule;

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apollo.common.parameter.CommonParameter;
import org.apollo.common.utils.Sha256Hash;


@Slf4j(topic = "capsule")
public class StorageRowCapsule implements ProtoCapsule<byte[]> {

  @Getter
  private byte[] rowValue;
  @Setter
  @Getter
  private byte[] rowKey;

  @Getter
  private boolean dirty = false;

  public StorageRowCapsule(StorageRowCapsule rowCapsule) {
    this.rowKey = rowCapsule.getRowKey().clone();
    this.rowValue = rowCapsule.getRowValue().clone();
    this.dirty = rowCapsule.isDirty();
  }

  public StorageRowCapsule(byte[] rowKey, byte[] rowValue) {
    this.rowKey = rowKey;
    this.rowValue = rowValue;
    markDirty();
  }

  public StorageRowCapsule(byte[] rowValue) {
    this.rowValue = rowValue;
  }

  private void markDirty() {
    dirty = true;
  }

  public Sha256Hash getHash() {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        this.rowValue);
  }

  public byte[] getValue() {
    return this.rowValue;
  }

  public void setValue(byte[] value) {
    this.rowValue = value;
    markDirty();
  }

  @Override
  public byte[] getData() {
    return this.rowValue;
  }

  @Override
  public byte[] getInstance() {
    return this.rowValue;
  }

  @Override
  public String toString() {
    return Arrays.toString(rowValue);
  }
}
