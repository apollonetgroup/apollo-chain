package org.apollo.core.services.http;

import com.google.protobuf.ByteString;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI.AccountNetMessage;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class GetAccountNetServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String address = request.getParameter("address");
      if (visible) {
        address = Util.getHexAddress(address);
      }
      fillResponse(visible, ByteString.copyFrom(ByteArray.fromHexString(address)), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      Account.Builder build = Account.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(), build.getAddress(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, ByteString address, HttpServletResponse response)
      throws Exception {
    AccountNetMessage reply = wallet.getAccountNet(address);
    if (reply != null) {
      response.getWriter().println(JsonFormat.printToString(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
