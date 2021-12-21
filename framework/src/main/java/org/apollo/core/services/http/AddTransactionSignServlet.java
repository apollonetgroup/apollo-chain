package org.apollo.core.services.http;

import com.alibaba.fastjson.JSONObject;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.capsule.TransactionCapsule;
import org.apollo.core.utils.TransactionUtil;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.Protocol.TransactionSign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class AddTransactionSignServlet extends RateLimiterServlet {

  @Autowired
  private TransactionUtil transactionUtil;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);
      JSONObject input = JSONObject.parseObject(contract);
      boolean visible = Util.getVisibleOnlyForSign(input);
      String strTransaction = input.getJSONObject("transaction").toJSONString();
      Transaction transaction = Util.packTransaction(strTransaction, visible);
      JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction,
          visible));
      input.put("transaction", jsonTransaction);
      TransactionSign.Builder build = TransactionSign.newBuilder();
      JsonFormat.merge(input.toJSONString(), build, visible);
      TransactionCapsule reply = transactionUtil.addSign(build.build());
      if (reply != null) {
        response.getWriter().println(Util.printCreateTransaction(reply.getInstance(), visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
