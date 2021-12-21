package org.apollo.core.net;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.services.DelegationServiceTest;
import org.apollo.core.services.NodeInfoServiceTest;
import org.junit.Test;

@Slf4j
public class BaseNetTest extends BaseNet {

  @Test
  public void test() throws Exception {
    new NodeInfoServiceTest(context).test();
    new UdpTest(context).test();
    new TcpTest(context).test();
    new DelegationServiceTest(context).test();
  }
}
