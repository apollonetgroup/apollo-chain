package org.apollo.core.services.interfaceOnPBFT.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apollo.core.services.http.GetMarketOrderListByPairServlet;
import org.apollo.core.services.interfaceOnPBFT.WalletOnPBFT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j(topic = "API")
public class GetMarketOrderListByPairOnPBFTServlet extends GetMarketOrderListByPairServlet {

  @Autowired
  private WalletOnPBFT walletOnPBFT;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    walletOnPBFT.futureGet(() -> super.doGet(request, response));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnPBFT.futureGet(() -> super.doPost(request, response));
  }
}
