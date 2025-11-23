package com.bitunix.scalper.service;

import com.bitunix.scalper.util.BybitApiAuthUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Bybit Demo Trading API
 * Implements demo trading functionality according to Bybit v5 API documentation
 */
@Service
public class BybitDemoTradingService {
    
    @Value("${bitunix.api.base-url:https://api-demo.bybit.com}")
    private String baseUrl;
    
    @Value("${bitunix.api.api-key}")
    private String apiKey;
    
    @Value("${bitunix.api.secret-key}")
    private String secretKey;
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long RECV_WINDOW = 5000; // 5 seconds
    
    /**
     * Create HTTP client with timeout configuration
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .build();
        
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * Add authentication headers to request
     */
    private void addAuthHeaders(HttpPost request, String requestBody) {
        long timestamp = BybitApiAuthUtil.getTimestamp();
        String signature = BybitApiAuthUtil.generateSignature(
            secretKey, timestamp, RECV_WINDOW, apiKey, "", requestBody);
        
        request.setHeader("X-BAPI-API-KEY", apiKey);
        request.setHeader("X-BAPI-TIMESTAMP", String.valueOf(timestamp));
        request.setHeader("X-BAPI-RECV-WINDOW", String.valueOf(RECV_WINDOW));
        request.setHeader("X-BAPI-SIGN", signature);
        request.setHeader("Content-Type", "application/json");
    }
    
    private void addAuthHeaders(HttpGet request, String queryString) {
        long timestamp = BybitApiAuthUtil.getTimestamp();
        String signature = BybitApiAuthUtil.generateSignature(
            secretKey, timestamp, RECV_WINDOW, apiKey, queryString, "");
        
        request.setHeader("X-BAPI-API-KEY", apiKey);
        request.setHeader("X-BAPI-TIMESTAMP", String.valueOf(timestamp));
        request.setHeader("X-BAPI-RECV-WINDOW", String.valueOf(RECV_WINDOW));
        request.setHeader("X-BAPI-SIGN", signature);
    }
    
    /**
     * Request demo trading funds
     * POST /v5/account/demo-apply-money
     * Rate limit: 1 req per minute
     * 
     * @param adjustType 0: add funds, 1: reduce funds
     * @param demoApplyMoney Array of coins and amounts to apply
     * @return Response JSON node
     */
    public JsonNode requestDemoFunds(int adjustType, Map<String, String> demoApplyMoney) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for demo funds request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("adjustType", adjustType);
            
            // Build utaDemoApplyMoney array
            java.util.List<Map<String, String>> utaDemoApplyMoney = new java.util.ArrayList<>();
            for (Map.Entry<String, String> entry : demoApplyMoney.entrySet()) {
                Map<String, String> coinAmount = new HashMap<>();
                coinAmount.put("coin", entry.getKey());
                coinAmount.put("amountStr", entry.getValue());
                utaDemoApplyMoney.add(coinAmount);
            }
            requestBodyMap.put("utaDemoApplyMoney", utaDemoApplyMoney);
            
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            
            HttpPost request = new HttpPost(baseUrl + "/v5/account/demo-apply-money");
            addAuthHeaders(request, requestBody);
            request.setEntity(new StringEntity(requestBody, "UTF-8"));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("Successfully requested demo funds");
                    return jsonNode;
                } else {
                    System.err.println("Error requesting demo funds: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error requesting demo funds: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get wallet balance
     * GET /v5/account/wallet-balance
     * 
     * @param accountType Account type (e.g., "UNIFIED")
     * @return Wallet balance JSON node
     */
    public JsonNode getWalletBalance(String accountType) {
        // Check rate limiter - if limit exceeded, return null (non-blocking)
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for wallet balance request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String queryString = "accountType=" + accountType;
            String url = baseUrl + "/v5/account/wallet-balance?" + queryString;
            
            HttpGet request = new HttpGet(url);
            addAuthHeaders(request, queryString);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    return jsonNode;
                } else {
                    System.err.println("Error getting wallet balance: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting wallet balance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Place order
     * POST /v5/order/create
     * 
     * @param category Product category (e.g., "linear", "spot")
     * @param symbol Trading symbol (e.g., "BTCUSDT")
     * @param side Order side ("Buy" or "Sell")
     * @param orderType Order type (e.g., "Market", "Limit")
     * @param qty Order quantity
     * @param price Order price (for limit orders)
     * @return Order response JSON node
     */
    public JsonNode placeOrder(String category, String symbol, String side, 
                              String orderType, String qty, String price) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("category", category);
            requestBodyMap.put("symbol", symbol);
            requestBodyMap.put("side", side);
            requestBodyMap.put("orderType", orderType);
            requestBodyMap.put("qty", qty);
            
            if (price != null && !price.isEmpty()) {
                requestBodyMap.put("price", price);
            }
            
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            
            HttpPost request = new HttpPost(baseUrl + "/v5/order/create");
            addAuthHeaders(request, requestBody);
            request.setEntity(new StringEntity(requestBody, "UTF-8"));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("Order placed successfully: " + symbol);
                    return jsonNode;
                } else {
                    System.err.println("Error placing order: " + responseBody);
                    return jsonNode; // Return error response for inspection
                }
            }
        } catch (Exception e) {
            System.err.println("Error placing order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get open orders
     * GET /v5/order/realtime
     * 
     * @param category Product category
     * @param symbol Trading symbol (optional)
     * @return Open orders JSON node
     */
    public JsonNode getOpenOrders(String category, String symbol) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("category=").append(category);
            if (symbol != null && !symbol.isEmpty()) {
                queryBuilder.append("&symbol=").append(symbol);
            }
            String queryString = queryBuilder.toString();
            
            String url = baseUrl + "/v5/order/realtime?" + queryString;
            HttpGet request = new HttpGet(url);
            addAuthHeaders(request, queryString);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    return jsonNode;
                } else {
                    System.err.println("Error getting open orders: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting open orders: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Cancel order
     * POST /v5/order/cancel
     * 
     * @param category Product category
     * @param symbol Trading symbol
     * @param orderId Order ID (optional)
     * @param orderLinkId Order link ID (optional)
     * @return Cancel response JSON node
     */
    public JsonNode cancelOrder(String category, String symbol, String orderId, String orderLinkId) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("category", category);
            requestBodyMap.put("symbol", symbol);
            
            if (orderId != null && !orderId.isEmpty()) {
                requestBodyMap.put("orderId", orderId);
            }
            if (orderLinkId != null && !orderLinkId.isEmpty()) {
                requestBodyMap.put("orderLinkId", orderLinkId);
            }
            
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            
            HttpPost request = new HttpPost(baseUrl + "/v5/order/cancel");
            addAuthHeaders(request, requestBody);
            request.setEntity(new StringEntity(requestBody, "UTF-8"));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("Order cancelled successfully");
                    return jsonNode;
                } else {
                    System.err.println("Error cancelling order: " + responseBody);
                    return jsonNode;
                }
            }
        } catch (Exception e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get position list
     * GET /v5/position/list
     * 
     * @param category Product category
     * @param symbol Trading symbol (optional)
     * @return Position list JSON node
     */
    public JsonNode getPositions(String category, String symbol) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("category=").append(category);
            if (symbol != null && !symbol.isEmpty()) {
                queryBuilder.append("&symbol=").append(symbol);
            }
            String queryString = queryBuilder.toString();
            
            String url = baseUrl + "/v5/position/list?" + queryString;
            HttpGet request = new HttpGet(url);
            addAuthHeaders(request, queryString);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    return jsonNode;
                } else {
                    System.err.println("Error getting positions: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting positions: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get account info
     * GET /v5/account/info
     * 
     * @return Account info JSON node
     */
    public JsonNode getAccountInfo() {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(baseUrl + "/v5/account/info");
            addAuthHeaders(request, "");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    return jsonNode;
                } else {
                    System.err.println("Error getting account info: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting account info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get market tickers (public endpoint, no auth required)
     * GET /v5/market/tickers
     * 
     * @param category Product category
     * @param symbol Trading symbol (optional)
     * @return Market tickers JSON node
     */
    public JsonNode getMarketTickers(String category, String symbol) {
        if (!rateLimiterService.canMakeRequest("bybit_demo")) {
            System.out.println("Rate limit exceeded for bybit_demo request");
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("category=").append(category);
            if (symbol != null && !symbol.isEmpty()) {
                queryBuilder.append("&symbol=").append(symbol);
            }
            
            String url = baseUrl + "/v5/market/tickers?" + queryBuilder.toString();
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    return jsonNode;
                } else {
                    System.err.println("Error getting market tickers: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting market tickers: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

