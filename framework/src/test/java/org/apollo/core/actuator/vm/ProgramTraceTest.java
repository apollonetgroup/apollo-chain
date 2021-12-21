package org.apollo.core.actuator.vm;

import java.io.File;
import java.math.BigInteger;
import java.util.List;

import org.apollo.common.runtime.vm.DataWord;
import org.apollo.common.utils.FileUtil;
import org.apollo.core.Constant;
import org.apollo.core.config.args.Args;
import org.apollo.core.vm.OpCode;
import org.apollo.core.vm.trace.Op;
import org.apollo.core.vm.trace.OpActions;
import org.apollo.core.vm.trace.ProgramTrace;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProgramTraceTest {
  private static final String dbPath = "output_programTrace_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void programTraceTest() {

    ProgramTrace programTrace = new ProgramTrace();
    ProgramTrace anotherProgramTrace = new ProgramTrace();
    DataWord energyDataWord = new DataWord(4);
    OpActions opActions = new OpActions();
    byte addOpCode = OpCode.ADD.val();
    byte subOpCode = OpCode.SUB.val();
    programTrace.addOp(addOpCode, 2, 3, energyDataWord, opActions);
    anotherProgramTrace.addOp(subOpCode, 5, 6, energyDataWord, opActions);

    programTrace.merge(anotherProgramTrace);

    List<Op> ops = programTrace.getOps();
    Assert.assertFalse(ops.isEmpty());
    Assert.assertEquals(2, ops.size());
    for (Op op : ops) {
      if (op.getCode() == OpCode.ADD) {
        Assert.assertEquals(3, op.getDeep());
        Assert.assertEquals(2, op.getPc());
        Assert.assertEquals(BigInteger.valueOf(4), op.getEnergy());
      } else if (op.getCode() == OpCode.SUB) {
        Assert.assertEquals(6, op.getDeep());
        Assert.assertEquals(5, op.getPc());
        Assert.assertEquals(BigInteger.valueOf(4), op.getEnergy());
      } else {
        Assert.fail("Invalid op code");
      }
    }
  }


}
