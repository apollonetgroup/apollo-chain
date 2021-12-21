package org.apollo.core.services.http;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.Wallet;
import org.apollo.protos.Protocol.Transaction;
import org.apollo.protos.Protocol.Transaction.Contract.ContractType;
import org.apollo.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class UpdateSettingServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      UpdateSettingContract.Builder build = UpdateSettingContract.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.UpdateSettingContract)
          .getInstance();
      JSONObject jsonObject = JSONObject.parseObject(params.getParams());
      tx = Util.setTransactionPermissionId(jsonObject, tx);

      response.getWriter().println(Util.printCreateTransaction(tx, params.isVisible()));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
