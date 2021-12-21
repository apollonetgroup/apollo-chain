package org.apollo.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI;
import org.apollo.common.utils.ByteArray;
import org.apollo.core.Wallet;
import org.apollo.core.zen.address.DiversifierT;
import org.apollo.core.zen.address.IncomingViewingKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class GetZenPaymentAddressServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      JSONObject jsonObject = JSONObject.parseObject(input);
      boolean visible = Util.getVisiblePost(input);

      String ivk = jsonObject.getString("ivk");
      String d = jsonObject.getString("d");

      GrpcAPI.PaymentAddressMessage s = wallet
          .getPaymentAddress(new IncomingViewingKey(ByteArray.fromHexString(ivk)),
              new DiversifierT(ByteArray.fromHexString(d)));

      response.getWriter()
          .println(JsonFormat.printToString(s, visible));

    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String ivk = request.getParameter("ivk");
      String d = request.getParameter("d");

      GrpcAPI.PaymentAddressMessage s = wallet
          .getPaymentAddress(new IncomingViewingKey(ByteArray.fromHexString(ivk)),
              new DiversifierT(ByteArray.fromHexString(d)));

      response.getWriter()
          .println(JsonFormat.printToString(s, visible));

    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
