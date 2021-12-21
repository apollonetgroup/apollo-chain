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
package org.apollo.core.vm.trace;

import static java.lang.String.format;
import static org.apollo.common.utils.ByteArray.toHexString;
import static org.apollo.core.db.TransactionTrace.convertToTronAddress;
import static org.apollo.core.vm.trace.Serializers.serializeFieldsOnly;

import java.util.ArrayList;
import java.util.List;

import org.apollo.common.runtime.vm.DataWord;
import org.apollo.core.vm.OpCode;
import org.apollo.core.vm.config.VMConfig;
import org.apollo.core.vm.program.invoke.ProgramInvoke;
import org.bouncycastle.util.encoders.Hex;

public class ProgramTrace {

  private List<Op> ops = new ArrayList<>();
  private String result;
  private String error;
  private String contractAddress;

  public ProgramTrace() {
    this(null, null);
  }

  public ProgramTrace(VMConfig config, ProgramInvoke programInvoke) {
    if (programInvoke != null && config.vmTrace()) {
      contractAddress = Hex
          .toHexString(convertToTronAddress(programInvoke.getContractAddress().getLast20Bytes()));
    }
  }

  public List<Op> getOps() {
    return ops;
  }

  public void setOps(List<Op> ops) {
    this.ops = ops;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public ProgramTrace result(byte[] result) {
    setResult(toHexString(result));
    return this;
  }

  public ProgramTrace error(Exception error) {
    setError(error == null ? "" : format("%s: %s", error.getClass(), error.getMessage()));
    return this;
  }

  public Op addOp(byte code, int pc, int deep, DataWord energy, OpActions actions) {
    Op op = new Op();
    op.setActions(actions);
    op.setCode(OpCode.code(code));
    op.setDeep(deep);
    op.setEnergy(energy.value());
    op.setPc(pc);

    ops.add(op);

    return op;
  }

  /**
   * Used for merging sub calls execution.
   */
  public void merge(ProgramTrace programTrace) {
    this.ops.addAll(programTrace.ops);
  }

  public String asJsonString(boolean formatted) {
    return serializeFieldsOnly(this, formatted);
  }

  @Override
  public String toString() {
    return asJsonString(true);
  }
}
