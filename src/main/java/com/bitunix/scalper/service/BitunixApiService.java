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
     * Get all trading pairs from Bitunix
     */
    public List<TradingPair> getAllTradingPairs() {
        List<TradingPair> pairs = new ArrayList<>();
        
        // Always try to get real data first, fallback to demo if fails
        // if (apiKey == null || apiKey.equals("your-api-key") || apiKey.isEmpty()) {
        //     return getDemoTradingPairs();
        // }
        
        // Проверяем Rate Limiter перед запросом
        rateLimiterService.waitIfNeeded("bitunix");
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            // Try public API first (no authentication required)
            HttpGet request = new HttpGet("https://api.bitunix.com/api/v1/ticker/24hr");
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
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
            }
        } catch (Exception e) {
            // Log error and try alternative API
            System.err.println("Error fetching trading pairs from primary API: " + e.getMessage());
            return tryAlternativeApi();
        }
        
        return pairs;
    }
    
    /**
     * Get specific trading pair data
     */
    public TradingPair getTradingPair(String symbol) {
        // Проверяем Rate Limiter перед запросом
        rateLimiterService.waitIfNeeded("bitunix");
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(baseUrl + "/api/v1/ticker/24hr?symbol=" + symbol);
            request.setHeader("X-MBX-APIKEY", apiKey);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                return parseTradingPair(jsonNode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching trading pair " + symbol + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get kline/candlestick data for technical analysis
     */
    public List<TradingPair> getKlineData(String symbol, String interval, int limit) {
        List<TradingPair> klines = new ArrayList<>();
        
        // Проверяем Rate Limiter перед запросом
        rateLimiterService.waitIfNeeded("bitunix");
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            String url = String.format("%s/api/v1/klines?symbol=%s&interval=%s&limit=%d", 
                                     baseUrl, symbol, interval, limit);
            HttpGet request = new HttpGet(url);
            request.setHeader("X-MBX-APIKEY", apiKey);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                if (jsonNode.isArray()) {
                    for (JsonNode klineNode : jsonNode) {
                        TradingPair pair = parseKlineData(klineNode, symbol);
                        if (pair != null) {
                            klines.add(pair);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching kline data for " + symbol + ": " + e.getMessage());
        }
        
        return klines;
    }
    
    /**
     * Parse trading pair from JSON response
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
     * Parse kline data from JSON response
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
            rateLimiterService.waitIfNeeded("bitunix");
            
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
