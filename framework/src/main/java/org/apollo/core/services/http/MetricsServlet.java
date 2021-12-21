package org.apollo.core.services.http;

import com.alibaba.fastjson.JSON;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.metrics.MetricsApiService;
import org.apollo.core.metrics.MetricsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "API")
public class MetricsServlet extends RateLimiterServlet {

  @Autowired
  private MetricsApiService metricsApiService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      MetricsInfo metricsInfo = metricsApiService.getMetricsInfo();

      if (metricsInfo != null) {
        response.getWriter().println(JSON.toJSONString(metricsInfo, true));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
