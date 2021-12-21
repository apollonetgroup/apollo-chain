package org.apollo.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.db.Manager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class GetBrokerageServlet extends RateLimiterServlet {

  @Autowired
  private Manager manager;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      int value = 0;
      byte[] address = Util.getAddress(request);
      long cycle = manager.getDynamicPropertiesStore().getCurrentCycleNumber();
      if (address != null) {
        value = manager.getDelegationStore().getBrokerage(cycle, address);
      }
      response.getWriter().println("{\"brokerage\": " + value + "}");
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}
