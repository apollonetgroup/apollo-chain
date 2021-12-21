package stest.apollo.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apollo.common.crypto.ECKey;
import org.apollo.common.utils.ByteArray;
import org.apollo.common.utils.Utils;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import stest.apollo.wallet.common.client.Configuration;
import stest.apollo.wallet.common.client.utils.Base58;
import stest.apollo.wallet.common.client.utils.HttpMethed;
import stest.apollo.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestMarket001 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "testAssetIssue002_" + now;
  private static String assetIssueId1;
  private static String assetIssueId2;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sellAddress = ecKey1.getAddress();
  String sellKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  String txId1;
  String txId2;
  String orderId;
  String orderId1;
  String orderId2;

  Long amount = 2048000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private JSONObject responseContent;
  private JSONObject getMarketOrderByIdContent;
  private JSONObject getMarketOrderByIdContentFromSolidity;
  private JSONObject getMarketOrderByIdContentFromPbft;
  private JSONObject getMarketOrderByAccountContent;
  private JSONObject getMarketOrderByAccountContentFromSolidity;
  private JSONObject getMarketOrderByAccountContentFromPbft;
  private JSONObject getMarketPairListContent;
  private JSONObject getMarketPairListContentFromSolidity;
  private JSONObject getMarketPairListContentFromPbft;
  private JSONObject getMarketOrderListByPairContent;
  private JSONObject getMarketOrderListByPairContentFromSolidity;
  private JSONObject getMarketOrderListByPairContentFromPbft;
  private JSONObject getMarketPriceByPairContent;
  private JSONObject getMarketPriceByPairContentFromSolidity;
  private JSONObject getMarketPriceByPairContentFromPbft;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(1);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  /**
   * constructor.
   */
  @Test(enabled = true, description = "MarketSellAsset trx with trc10 by http")
  public void test01MarketSellAsset() {
    PublicMethed.printAddress(sellKey);
    PublicMethed.printAddress(dev002Key);

    response = HttpMethed.sendCoin(httpnode, fromAddress, sellAddress, amount, testKey002);
    response = HttpMethed.sendCoin(httpnode, fromAddress, dev002Address, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    //Create an asset issue
    response = HttpMethed.assetIssue(httpnode, sellAddress, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, sellKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, sellAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId1 = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId1);
    Assert.assertTrue(Integer.parseInt(assetIssueId1) > 1000000);

    response = HttpMethed.assetIssue(httpnode, dev002Address, name, name, totalSupply, 1, 1,
        System.currentTimeMillis() + 5000, System.currentTimeMillis() + 50000000, 2, 3, description,
        url, 1000L, 1000L, dev002Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, dev002Address);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    assetIssueId2 = responseContent.getString("asset_issued_ID");
    logger.info(assetIssueId2);
    Assert.assertTrue(Integer.parseInt(assetIssueId2) > 1000000);

    // transferAsset
    response = HttpMethed
        .transferAsset(httpnode, dev002Address, sellAddress, assetIssueId2, 10000L, dev002Key);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, sellAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    // marketsellasset trc10-trc10
    txId2 = HttpMethed
        .marketSellAssetGetTxId(httpnode, sellAddress, assetIssueId1, 10L, assetIssueId2, 500L,
            sellKey, "true");
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txId2);
    response = HttpMethed.getTransactionInfoById(httpnode, txId2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    orderId = responseContent.getString("orderId");
    logger.info("orderId:" + orderId);

    // marketsellasset trx-trc10
    txId1 = HttpMethed
        .marketSellAssetGetTxId(httpnode, sellAddress, "_", 1000L, assetIssueId1, 20L, sellKey,
            "false");
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txId1);
    response = HttpMethed.getTransactionInfoById(httpnode, txId1);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    orderId1 = responseContent.getString("orderId");
    logger.info("orderId1:" + orderId1);

    // marketsellasset trc10-trx
    txId2 = HttpMethed
        .marketSellAssetGetTxId(httpnode, sellAddress, assetIssueId1, 10L, "_", 500L, sellKey,
            "true");
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txId2);
    response = HttpMethed.getTransactionInfoById(httpnode, txId2);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONObject orderDetails = responseContent.getJSONArray("orderDetails").getJSONObject(0);
    Assert.assertTrue(!responseContent.getString("orderId").isEmpty());
    Assert.assertTrue(500L == orderDetails.getLong("fillBuyQuantity"));
    Assert.assertTrue(10L == orderDetails.getLong("fillSellQuantity"));
    Assert
        .assertEquals(responseContent.getString("orderId"), orderDetails.getString("takerOrderId"));
    Assert.assertEquals(orderId1, orderDetails.getString("makerOrderId"));
    orderId2 = responseContent.getString("orderId");
    logger.info("orderId2:" + orderId2);


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderById by http")
  public void test02GetMarketOrderById() {
    // getMarketOrderById orderId1
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getMarketOrderById(httpnode, orderId1, "true");
    getMarketOrderByIdContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByIdContent);
    Assert.assertEquals(Base58.encode58Check(sellAddress),
        getMarketOrderByIdContent.getString("owner_address"));
    Assert.assertEquals("_", getMarketOrderByIdContent.getString("sell_token_id"));
    Assert.assertTrue(1000L == getMarketOrderByIdContent.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, getMarketOrderByIdContent.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContent.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == getMarketOrderByIdContent.getLong("sell_token_quantity_remain"));

    // getMarketOrderById orderId2
    HttpResponse response2 = HttpMethed.getMarketOrderById(httpnode, orderId2, "false");
    JSONObject getMarketOrderByIdContent2 = HttpMethed.parseResponseContent(response2);
    HttpMethed.printJsonContent(getMarketOrderByIdContent2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderById by http from solidity")
  public void test03GetMarketOrderByIdFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSolidityNode);
    response = HttpMethed.getMarketOrderByIdFromSolidity(httpSolidityNode, orderId1, "true");
    getMarketOrderByIdContentFromSolidity = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByIdContentFromSolidity);
    Assert.assertEquals(Base58.encode58Check(sellAddress),
        getMarketOrderByIdContentFromSolidity.getString("owner_address"));
    Assert.assertEquals("_", getMarketOrderByIdContentFromSolidity.getString("sell_token_id"));
    Assert
        .assertTrue(1000L == getMarketOrderByIdContentFromSolidity.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1,
        getMarketOrderByIdContentFromSolidity.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContentFromSolidity.getLong("buy_token_quantity"));
    Assert.assertTrue(
        500L == getMarketOrderByIdContentFromSolidity.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByIdContent, getMarketOrderByIdContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderById by http from pbft")
  public void test04GetMarketOrderByIdFromPbft() {
    HttpMethed.waitToProduceOneBlockFromPbft(httpnode, httpPbftNode);
    response = HttpMethed.getMarketOrderByIdFromPbft(httpPbftNode, orderId1, "true");
    getMarketOrderByIdContentFromPbft = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByIdContentFromPbft);
    Assert.assertEquals(Base58.encode58Check(sellAddress),
        getMarketOrderByIdContentFromPbft.getString("owner_address"));
    Assert.assertEquals("_", getMarketOrderByIdContentFromPbft.getString("sell_token_id"));
    Assert
        .assertTrue(1000L == getMarketOrderByIdContentFromPbft.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1,
        getMarketOrderByIdContentFromPbft.getString("buy_token_id"));
    Assert.assertTrue(20L == getMarketOrderByIdContentFromPbft.getLong("buy_token_quantity"));
    Assert.assertTrue(
        500L == getMarketOrderByIdContentFromPbft.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByIdContent, getMarketOrderByIdContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderByAccount by http")
  public void test05GetMarketOrderByAccount() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getMarketOrderByAccount(httpnode, sellAddress, "true");
    getMarketOrderByAccountContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContent.getJSONArray("orders").getJSONObject(1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderByAccount by http from solidity")
  public void test06GetMarketOrderByAccountFromSolidity() {
    response = HttpMethed
        .getMarketOrderByAccountFromSolidity(httpSolidityNode, sellAddress, "true");
    getMarketOrderByAccountContentFromSolidity = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByAccountContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContentFromSolidity.getJSONArray("orders")
        .getJSONObject(1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByAccountContent, getMarketOrderByAccountContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderByAccount by http from pbft")
  public void test07GetMarketOrderByAccountFromPbft() {
    response = HttpMethed.getMarketOrderByAccountFromPbft(httpPbftNode, sellAddress, "true");
    getMarketOrderByAccountContentFromPbft = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByAccountContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderByAccountContentFromPbft.getJSONArray("orders")
        .getJSONObject(1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertTrue(500L == orders.getLong("sell_token_quantity_remain"));
    Assert.assertEquals(getMarketOrderByAccountContent, getMarketOrderByAccountContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPairList by http")
  public void test08GetMarketPairList() {
    response = HttpMethed.getMarketPairList(httpnode, "true");
    getMarketPairListContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPairListContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContent.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("_",
        getMarketPairListContent.getJSONArray("orderPair").getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(assetIssueId1,
        getMarketPairListContent.getJSONArray("orderPair").getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPairList by http from solidity")
  public void test09GetMarketPairListFromSolidity() {
    response = HttpMethed.getMarketPairListFromSolidity(httpSolidityNode, "true");
    getMarketPairListContentFromSolidity = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPairListContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContentFromSolidity.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("_",
        getMarketPairListContentFromSolidity.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(assetIssueId1,
        getMarketPairListContentFromSolidity.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
    Assert.assertEquals(getMarketPairListContent, getMarketPairListContentFromSolidity);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPairList by http from pbft")
  public void test10GetMarketPairListFromPbft() {
    response = HttpMethed.getMarketPairListFromPbft(httpPbftNode, "true");
    getMarketPairListContentFromPbft = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPairListContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    int orderPairSize = getMarketPairListContentFromPbft.getJSONArray("orderPair").size();
    Assert.assertTrue(orderPairSize > 0);
    Assert.assertEquals("_",
        getMarketPairListContentFromPbft.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("sell_token_id"));
    Assert.assertEquals(assetIssueId1,
        getMarketPairListContentFromPbft.getJSONArray("orderPair")
            .getJSONObject(orderPairSize - 1)
            .getString("buy_token_id"));
    Assert.assertEquals(getMarketPairListContent, getMarketPairListContentFromPbft);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderListByPair by http")
  public void test11GetMarketOrderListByPair() {
    response = HttpMethed.getMarketOrderListByPair(httpnode, "_", assetIssueId1, "true");
    getMarketOrderListByPairContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderListByPairContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContent.getJSONArray("orders")
        .getJSONObject(getMarketOrderListByPairContent.getJSONArray("orders").size() - 1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContent.getLong("sell_token_quantity"),
        getMarketOrderListByPairContent.getLong("sell_token_quantity_remain"));

    Assert.assertTrue(getMarketOrderListByPairContent.getJSONArray("orders").size() > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderListByPair by http from solidity")
  public void test12GetMarketOrderListByPairFromSolidity() {
    response = HttpMethed
        .getMarketOrderListByPairFromSolidity(httpSolidityNode, "_", assetIssueId1, "true");
    getMarketOrderListByPairContentFromSolidity = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderListByPairContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContentFromSolidity.getJSONArray("orders")
        .getJSONObject(
            getMarketOrderListByPairContentFromSolidity.getJSONArray("orders").size() - 1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContentFromSolidity.getLong("sell_token_quantity"),
        getMarketOrderListByPairContentFromSolidity.getLong("sell_token_quantity_remain"));

    Assert
        .assertTrue(getMarketOrderListByPairContentFromSolidity.getJSONArray("orders").size() > 0);
    Assert
        .assertEquals(getMarketOrderListByPairContent, getMarketOrderListByPairContentFromSolidity);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketOrderListByPair by http from pbft")
  public void test13GetMarketOrderListByPairFromPbft() {
    response = HttpMethed
        .getMarketOrderListByPairFromPbft(httpPbftNode, "_", assetIssueId1, "true");
    getMarketOrderListByPairContentFromPbft = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderListByPairContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject orders = getMarketOrderListByPairContentFromPbft.getJSONArray("orders")
        .getJSONObject(
            getMarketOrderListByPairContentFromPbft.getJSONArray("orders").size() - 1);
    Assert.assertEquals(Base58.encode58Check(sellAddress), orders.getString("owner_address"));
    Assert.assertEquals("_", orders.getString("sell_token_id"));
    Assert.assertTrue(1000L == orders.getLong("sell_token_quantity"));
    Assert.assertEquals(assetIssueId1, orders.getString("buy_token_id"));
    Assert.assertTrue(20L == orders.getLong("buy_token_quantity"));
    Assert.assertEquals(getMarketOrderListByPairContentFromPbft.getLong("sell_token_quantity"),
        getMarketOrderListByPairContentFromPbft.getLong("sell_token_quantity_remain"));

    Assert
        .assertTrue(getMarketOrderListByPairContentFromPbft.getJSONArray("orders").size() > 0);
    Assert
        .assertEquals(getMarketOrderListByPairContent, getMarketOrderListByPairContentFromPbft);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPriceByPair from by http")
  public void test14GetMarketPriceByPair() {
    response = HttpMethed.getMarketPriceByPair(httpnode, "_", assetIssueId1, "true");
    getMarketPriceByPairContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPriceByPairContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("_", getMarketPriceByPairContent.getString("sell_token_id"));
    Assert.assertEquals(assetIssueId1, getMarketPriceByPairContent.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContent.getJSONArray("prices").getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContent.getJSONArray("prices").size() > 0);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPriceByPair from by http from solidity")
  public void test15GetMarketPriceByPairFromSolidity() {
    response = HttpMethed
        .getMarketPriceByPairFromSolidity(httpSolidityNode, "_", assetIssueId1, "true");
    getMarketPriceByPairContentFromSolidity = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPriceByPairContentFromSolidity);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("_", getMarketPriceByPairContentFromSolidity.getString("sell_token_id"));
    Assert
        .assertEquals(assetIssueId1,
            getMarketPriceByPairContentFromSolidity.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContentFromSolidity.getJSONArray("prices")
        .getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContentFromSolidity.getJSONArray("prices").size() > 0);
    Assert.assertEquals(getMarketPriceByPairContent, getMarketPriceByPairContentFromSolidity);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "GetMarketPriceByPair from by http from pbft")
  public void test16GetMarketPriceByPairFromPbft() {
    response = HttpMethed
        .getMarketPriceByPairFromPbft(httpPbftNode, "_", assetIssueId1, "true");
    getMarketPriceByPairContentFromPbft = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketPriceByPairContentFromPbft);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals("_", getMarketPriceByPairContentFromPbft.getString("sell_token_id"));
    Assert
        .assertEquals(assetIssueId1,
            getMarketPriceByPairContentFromPbft.getString("buy_token_id"));
    JSONObject prices = getMarketPriceByPairContentFromPbft.getJSONArray("prices")
        .getJSONObject(0);
    Assert.assertEquals("50", prices.getString("sell_token_quantity"));
    Assert.assertEquals("1", prices.getString("buy_token_quantity"));
    Assert.assertTrue(getMarketPriceByPairContentFromPbft.getJSONArray("prices").size() > 0);
    Assert.assertEquals(getMarketPriceByPairContent, getMarketPriceByPairContentFromPbft);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "MarketCancelOrder trx with trc10 by http")
  public void test17MarketCancelOrder() {
    response = HttpMethed.getMarketOrderByAccount(httpnode, sellAddress, "true");
    getMarketOrderByAccountContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals(2, getMarketOrderByAccountContent.getJSONArray("orders").size());

    // MarketCancelOrder
    String txId = HttpMethed.marketCancelOrder(httpnode, sellAddress, orderId1, sellKey, "true");
    HttpMethed.waitToProduceOneBlock(httpnode);
    logger.info(txId);
    response = HttpMethed.getTransactionInfoById(httpnode, txId);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

    response = HttpMethed.getMarketOrderByAccount(httpnode, sellAddress, "true");
    getMarketOrderByAccountContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(getMarketOrderByAccountContent);
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    Assert.assertEquals(1, getMarketOrderByAccountContent.getJSONArray("orders").size());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.freedResource(httpnode, sellAddress, fromAddress, sellKey);
    HttpMethed.disConnect();
  }
}