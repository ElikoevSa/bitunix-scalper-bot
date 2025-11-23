package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingPair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BitunixApiService {
    
    @Value("${bitunix.api.base-url}")
    private String baseUrl;
    
    @Value("${bitunix.api.api-key}")
    private String apiKey;
    
    @Value("${bitunix.api.secret-key}")
    private String secretKey;
    
    @Autowired
    private AlternativeDataService alternativeDataService;
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create HTTP client with timeout configuration
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000) // 5 seconds
                .setConnectTimeout(5000) // 5 seconds
                .setSocketTimeout(10000) // 10 seconds
                .build();
        
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * Get trading pairs for specific symbols from Bybit (v5 API)
     * If symbols list is empty, returns all pairs
     * Uses public market tickers endpoint - no authentication required
     * Makes a single request to get all tickers, then filters by selected symbols
     */
    public List<TradingPair> getTradingPairs(List<String> symbols) {
        // If no symbols specified, get all pairs
        if (symbols == null || symbols.isEmpty()) {
            return getAllTradingPairs();
        }
        
        // Check rate limiter - if limit exceeded, return empty list (non-blocking)
        if (!rateLimiterService.canMakeRequest("bitunix")) {
            System.out.println("Rate limit exceeded for trading pairs request, returning empty list");
            return new ArrayList<>();
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String apiUrl = baseUrl;
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api-demo.bybit.com";
            }
            if (!apiUrl.startsWith("http")) {
                apiUrl = "https://" + apiUrl;
            }
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            
            // Make a single request to get all tickers (more efficient than multiple requests)
            String tickersUrl = apiUrl + "/v5/market/tickers?category=linear";
            HttpGet request = new HttpGet(tickersUrl);
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    // Bybit v5 format: { "retCode": 0, "retMsg": "OK", "result": { "list": [...] } }
                    if (jsonNode.has("retCode") && jsonNode.get("retCode").asInt() == 0) {
                        JsonNode result = jsonNode.get("result");
                        if (result != null && result.has("list")) {
                            JsonNode list = result.get("list");
                            if (list.isArray()) {
                                // Convert selected symbols to set for faster lookup
                                java.util.Set<String> selectedSymbolsSet = new java.util.HashSet<>(symbols);
                                
                                // Parse all tickers and filter by selected symbols
                                List<TradingPair> pairs = new ArrayList<>();
                                for (JsonNode pairNode : list) {
                                    TradingPair pair = parseBybitV5Ticker(pairNode);
                                    if (pair != null && selectedSymbolsSet.contains(pair.getSymbol())) {
                                        pairs.add(pair);
                                    }
                                }
                                
                                if (!pairs.isEmpty()) {
                                    System.out.println("Successfully fetched " + pairs.size() + 
                                                     " trading pairs for " + symbols.size() + " selected symbols");
                                    return pairs;
                                }
                            }
                        }
                    } else {
                        System.err.println("Bybit API error: " + responseBody);
                    }
                } else {
                    System.err.println("HTTP error " + statusCode + ": " + responseBody);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching trading pairs: " + e.getMessage());
            e.printStackTrace();
        }
        
        // If failed, try alternative API
        System.out.println("No pairs received for selected symbols, trying alternative");
        return tryAlternativeApi();
    }
    
    /**
     * Get all trading pairs from Bybit (v5 API)
     * Uses public market tickers endpoint - no authentication required
     */
    public List<TradingPair> getAllTradingPairs() {
        List<TradingPair> pairs = new ArrayList<>();
        
        // Check rate limiter - if limit exceeded, return empty list (non-blocking)
        if (!rateLimiterService.canMakeRequest("bitunix")) {
            System.out.println("Rate limit exceeded for all trading pairs request, returning empty list");
            return new ArrayList<>();
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            // Use Bybit v5 market tickers endpoint (public, no auth required)
            // For demo: https://api-demo.bybit.com/v5/market/tickers
            // For mainnet: https://api.bybit.com/v5/market/tickers
            String apiUrl = baseUrl;
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api-demo.bybit.com";
            }
            // Ensure we use the correct base URL format
            if (!apiUrl.startsWith("http")) {
                apiUrl = "https://" + apiUrl;
            }
            // Remove trailing slash if present
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            
            String tickersUrl = apiUrl + "/v5/market/tickers?category=linear";
            HttpGet request = new HttpGet(tickersUrl);
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    // Bybit v5 format: { "retCode": 0, "retMsg": "OK", "result": { "list": [...] } }
                    if (jsonNode.has("retCode") && jsonNode.get("retCode").asInt() == 0) {
                        JsonNode result = jsonNode.get("result");
                        if (result != null && result.has("list")) {
                            JsonNode list = result.get("list");
                            if (list.isArray()) {
                                for (JsonNode pairNode : list) {
                                    TradingPair pair = parseBybitV5Ticker(pairNode);
                                    if (pair != null) {
                                        pairs.add(pair);
                                    }
                                }
                            }
                        }
                    } else {
                        System.err.println("Bybit API error: " + responseBody);
                    }
                } else {
                    System.err.println("HTTP error " + statusCode + ": " + responseBody);
                }
            }
        } catch (Exception e) {
            // Log error and try alternative API
            System.err.println("Error fetching trading pairs from Bybit v5 API: " + e.getMessage());
            e.printStackTrace();
            return tryAlternativeApi();
        }
        
        if (pairs.isEmpty()) {
            System.out.println("No pairs received from Bybit API, trying alternatives");
            return tryAlternativeApi();
        }
        
        System.out.println("Successfully fetched " + pairs.size() + " trading pairs from Bybit v5 API");
        return pairs;
    }
    
    /**
     * Get specific trading pair data from Bybit v5 API
     */
    public TradingPair getTradingPair(String symbol) {
        // Check rate limiter - if limit exceeded, return null (non-blocking)
        if (!rateLimiterService.canMakeRequest("bitunix")) {
            System.out.println("Rate limit exceeded for trading pair request: " + symbol);
            return null;
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String apiUrl = baseUrl;
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api-demo.bybit.com";
            }
            if (!apiUrl.startsWith("http")) {
                apiUrl = "https://" + apiUrl;
            }
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            
            String tickersUrl = apiUrl + "/v5/market/tickers?category=linear&symbol=" + symbol;
            HttpGet request = new HttpGet(tickersUrl);
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.has("retCode") && jsonNode.get("retCode").asInt() == 0) {
                        JsonNode result = jsonNode.get("result");
                        if (result != null && result.has("list") && result.get("list").isArray()) {
                            JsonNode list = result.get("list");
                            if (list.size() > 0) {
                                return parseBybitV5Ticker(list.get(0));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching trading pair " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get kline/candlestick data for technical analysis from Bybit v5 API
     */
    public List<TradingPair> getKlineData(String symbol, String interval, int limit) {
        List<TradingPair> klines = new ArrayList<>();
        
        // Check rate limiter - if limit exceeded, return empty list (non-blocking)
        if (!rateLimiterService.canMakeRequest("bitunix")) {
            System.out.println("Rate limit exceeded for kline data request: " + symbol);
            return new ArrayList<>();
        }
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String apiUrl = baseUrl;
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api-demo.bybit.com";
            }
            if (!apiUrl.startsWith("http")) {
                apiUrl = "https://" + apiUrl;
            }
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            
            // Map interval format (1m -> 1, 5m -> 5, 1h -> 60, 1d -> D)
            String bybitInterval = mapIntervalToBybit(interval);
            
            String klineUrl = String.format("%s/v5/market/kline?category=linear&symbol=%s&interval=%s&limit=%d", 
                                          apiUrl, symbol, bybitInterval, limit);
            HttpGet request = new HttpGet(klineUrl);
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.has("retCode") && jsonNode.get("retCode").asInt() == 0) {
                        JsonNode result = jsonNode.get("result");
                        if (result != null && result.has("list") && result.get("list").isArray()) {
                            JsonNode list = result.get("list");
                            for (JsonNode klineNode : list) {
                                TradingPair pair = parseBybitV5Kline(klineNode, symbol);
                                if (pair != null) {
                                    klines.add(pair);
                                }
                            }
                        }
                    }
                } else {
                    System.err.println("Error fetching kline data: HTTP " + statusCode + " - " + responseBody);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching kline data for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return klines;
    }
    
    /**
     * Map interval format to Bybit format
     */
    private String mapIntervalToBybit(String interval) {
        // Bybit supports: 1, 3, 5, 15, 30, 60, 120, 240, 360, 720, D, M, W
        switch (interval.toLowerCase()) {
            case "1m": return "1";
            case "3m": return "3";
            case "5m": return "5";
            case "15m": return "15";
            case "30m": return "30";
            case "1h": return "60";
            case "2h": return "120";
            case "4h": return "240";
            case "6h": return "360";
            case "12h": return "720";
            case "1d": return "D";
            case "1w": return "W";
            case "1M": return "M";
            default: return "1"; // Default to 1 minute
        }
    }
    
    /**
     * Parse trading pair from Bybit v5 ticker JSON response
     * Format: { "symbol": "BTCUSDT", "lastPrice": "45000.5", "volume24h": "1000000", "price24hPcnt": "2.5", ... }
     */
    private TradingPair parseBybitV5Ticker(JsonNode node) {
        try {
            TradingPair pair = new TradingPair();
            
            if (!node.has("symbol")) {
                return null;
            }
            
            pair.setSymbol(node.get("symbol").asText());
            
            // Parse price
            if (node.has("lastPrice")) {
                String priceStr = node.get("lastPrice").asText();
                if (priceStr != null && !priceStr.isEmpty()) {
                    pair.setPrice(new BigDecimal(priceStr));
                }
            }
            
            // Parse 24h volume
            if (node.has("volume24h")) {
                String volumeStr = node.get("volume24h").asText();
                if (volumeStr != null && !volumeStr.isEmpty()) {
                    pair.setVolume24h(new BigDecimal(volumeStr));
                }
            }
            
            // Parse 24h price change percentage
            if (node.has("price24hPcnt")) {
                String changeStr = node.get("price24hPcnt").asText();
                if (changeStr != null && !changeStr.isEmpty()) {
                    // Convert from decimal (0.025) to percentage (2.5)
                    BigDecimal change = new BigDecimal(changeStr);
                    pair.setPriceChange24h(change.multiply(BigDecimal.valueOf(100)));
                }
            }
            
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            // Extract base and quote assets from symbol
            String symbol = pair.getSymbol();
            if (symbol.endsWith("USDT")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 4));
                pair.setQuoteAsset("USDT");
            } else if (symbol.endsWith("USDC")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 4));
                pair.setQuoteAsset("USDC");
            } else if (symbol.endsWith("BTC")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 3));
                pair.setQuoteAsset("BTC");
            }
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing Bybit v5 ticker: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse trading pair from old format (for backward compatibility)
     */
    private TradingPair parseTradingPair(JsonNode node) {
        try {
            TradingPair pair = new TradingPair();
            pair.setSymbol(node.get("symbol").asText());
            pair.setPrice(new BigDecimal(node.get("lastPrice").asText()));
            pair.setVolume24h(new BigDecimal(node.get("volume").asText()));
            pair.setPriceChange24h(new BigDecimal(node.get("priceChangePercent").asText()));
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            // Extract base and quote assets from symbol
            String symbol = pair.getSymbol();
            if (symbol.endsWith("USDT")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 4));
                pair.setQuoteAsset("USDT");
            } else if (symbol.endsWith("BTC")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 3));
                pair.setQuoteAsset("BTC");
            }
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing trading pair: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse kline data from Bybit v5 API response
     * Format: ["1690000000000", "45000", "45100", "44900", "45050", "1000"]
     * [timestamp, open, high, low, close, volume]
     */
    private TradingPair parseBybitV5Kline(JsonNode node, String symbol) {
        try {
            if (!node.isArray() || node.size() < 6) {
                return null;
            }
            
            TradingPair pair = new TradingPair();
            pair.setSymbol(symbol);
            
            // Bybit v5 kline format: [timestamp, open, high, low, close, volume, turnover]
            // Index 4 is close price
            pair.setPrice(new BigDecimal(node.get(4).asText()));
            
            // Index 5 is volume
            if (node.size() > 5) {
                pair.setVolume24h(new BigDecimal(node.get(5).asText()));
            }
            
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing Bybit v5 kline data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse kline data from old format (for backward compatibility)
     */
    private TradingPair parseKlineData(JsonNode node, String symbol) {
        try {
            TradingPair pair = new TradingPair();
            pair.setSymbol(symbol);
            pair.setPrice(new BigDecimal(node.get(4).asText())); // Close price
            pair.setVolume24h(new BigDecimal(node.get(5).asText())); // Volume
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing kline data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get demo trading pairs for testing
     */
    private List<TradingPair> getDemoTradingPairs() {
        List<TradingPair> demoPairs = new ArrayList<>();
        
        // Create demo trading pairs
        String[] symbols = {"BTCUSDT", "ETHUSDT", "ADAUSDT", "DOTUSDT", "LINKUSDT"};
        double[] prices = {45000.0, 3000.0, 0.5, 25.0, 15.0};
        double[] volumes = {1000000.0, 800000.0, 500000.0, 300000.0, 200000.0};
        double[] changes = {2.5, -1.2, 3.8, -0.5, 1.5};
        
        for (int i = 0; i < symbols.length; i++) {
            TradingPair pair = new TradingPair();
            pair.setSymbol(symbols[i]);
            pair.setPrice(BigDecimal.valueOf(prices[i]));
            pair.setVolume24h(BigDecimal.valueOf(volumes[i]));
            pair.setPriceChange24h(BigDecimal.valueOf(changes[i]));
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            // Add some technical indicators
            pair.setRsi(BigDecimal.valueOf(30 + Math.random() * 40)); // RSI between 30-70
            pair.setBollingerUpper(BigDecimal.valueOf(prices[i] * 1.02));
            pair.setBollingerLower(BigDecimal.valueOf(prices[i] * 0.98));
            pair.setEma12(BigDecimal.valueOf(prices[i] * (0.99 + Math.random() * 0.02)));
            pair.setEma26(BigDecimal.valueOf(prices[i] * (0.98 + Math.random() * 0.04)));
            pair.setSupportLevel(BigDecimal.valueOf(prices[i] * 0.95));
            pair.setResistanceLevel(BigDecimal.valueOf(prices[i] * 1.05));
            
            demoPairs.add(pair);
        }
        
        return demoPairs;
    }
    
    /**
     * Try alternative API endpoints
     */
    private List<TradingPair> tryAlternativeApi() {
        List<TradingPair> pairs = new ArrayList<>();
        
        // Try Binance API first
        try {
            pairs = alternativeDataService.getBinanceData();
            if (!pairs.isEmpty()) {
                System.out.println("Successfully fetched " + pairs.size() + " trading pairs from Binance");
                return pairs;
            }
        } catch (Exception e) {
            System.err.println("Error fetching Binance data: " + e.getMessage());
        }
        
        // Try CoinGecko API
        try {
            pairs = alternativeDataService.getCoinGeckoData();
            if (!pairs.isEmpty()) {
                System.out.println("Successfully fetched " + pairs.size() + " trading pairs from CoinGecko");
                return pairs;
            }
        } catch (Exception e) {
            System.err.println("Error fetching CoinGecko data: " + e.getMessage());
        }
        
        // Try different Bitunix API endpoints
        String[] apiEndpoints = {
            "https://api.bitunix.com/api/v1/ticker/24hr",
            "https://api.bitunix.com/api/v1/ticker/price",
            "https://api.bitunix.com/api/v1/exchangeInfo"
        };
        
        for (String endpoint : apiEndpoints) {
            // Проверяем Rate Limiter перед каждым альтернативным запросом
            if (!rateLimiterService.canMakeRequest("bitunix")) {
                System.out.println("Rate limit exceeded for bitunix request");
                continue; // Skip this endpoint
            }
            
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpGet request = new HttpGet(endpoint);
                request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                request.setHeader("Accept", "application/json");
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        
                        if (jsonNode.isArray()) {
                            for (JsonNode pairNode : jsonNode) {
                                TradingPair pair = parseTradingPair(pairNode);
                                if (pair != null) {
                                    pairs.add(pair);
                                }
                            }
                        }
                        
                        if (!pairs.isEmpty()) {
                            System.out.println("Successfully fetched " + pairs.size() + " trading pairs from " + endpoint);
                            return pairs;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error with endpoint " + endpoint + ": " + e.getMessage());
            }
        }
        
        // If all APIs fail, return demo data
        System.out.println("All API endpoints failed, using demo data");
        return getDemoTradingPairs();
    }
}
