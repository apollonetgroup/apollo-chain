package org.apollo.core.services.http;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.api.GrpcAPI.NoteParameters;
import org.apollo.api.GrpcAPI.SpendResult;
import org.apollo.core.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class IsSpendServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams postParams = PostParams.getPostParams(request);
      boolean visible = postParams.isVisible();
      String input = postParams.getParams();
      NoteParameters.Builder build = NoteParameters.newBuilder();
      JsonFormat.merge(input, build, visible);

      SpendResult result = wallet.isSpend(build.build());
      response.getWriter().println(JsonFormat.printToString(result, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
