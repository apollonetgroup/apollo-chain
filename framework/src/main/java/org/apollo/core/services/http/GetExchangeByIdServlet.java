package org.apollo.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.common.utils.ByteArray;
import org.apollo.core.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class GetExchangeByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      long id = Util.getJsonLongValue(jsonObject, "id", true);
      fillResponse(params.isVisible(), id, response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("id");
      fillResponse(visible, Long.parseLong(input), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, long id, HttpServletResponse response)
      throws IOException {
    response.getWriter().println(JsonFormat.printToString(
        wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(id))), visible));
  }
}