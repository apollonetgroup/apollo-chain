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

package org.apollo.core.db;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.utils.Sha256Hash;
import org.apollo.core.capsule.BlockCapsule;
import org.apollo.core.capsule.BlockCapsule.BlockId;
import org.apollo.core.exception.BadItemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "DB")
@Component
public class BlockStore extends TronStoreWithRevoking<BlockCapsule> {

  @Autowired
  private BlockStore(@Value("block") String dbName) {
    super(dbName);
  }

  public List<BlockCapsule> getLimitNumber(long startNumber, long limit) {
    BlockId startBlockId = new BlockId(Sha256Hash.ZERO_HASH, startNumber);
    return revokingDB.getValuesNext(startBlockId.getBytes(), limit).stream()
        .map(bytes -> {
          try {
            return new BlockCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockCapsule::getNum))
        .collect(Collectors.toList());
  }

  public List<BlockCapsule> getBlockByLatestNum(long getNum) {

    return revokingDB.getlatestValues(getNum).stream()
        .map(bytes -> {
          try {
            return new BlockCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockCapsule::getNum))
        .collect(Collectors.toList());
  }
}
