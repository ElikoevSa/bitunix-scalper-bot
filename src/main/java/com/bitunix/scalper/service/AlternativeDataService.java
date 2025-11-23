package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingPair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlternativeDataService {
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get trading pairs from Binance API (as backup)
     */
    public List<TradingPair> getBinanceData() {
        List<TradingPair> pairs = new ArrayList<>();
        
        // Проверяем Rate Limiter перед запросом
        if (!rateLimiterService.canMakeRequest("binance")) {
            System.out.println("Rate limit exceeded for binance request");
            return new ArrayList<>();
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.binance.com/api/v3/ticker/24hr");
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.isArray()) {
                        for (JsonNode pairNode : jsonNode) {
                            TradingPair pair = parseBinancePair(pairNode);
                            if (pair != null) {
                                pairs.add(pair);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching Binance data: " + e.getMessage());
        }
        
        return pairs;
    }
    
    /**
     * Get trading pairs from CoinGecko API (as backup)
     */
    public List<TradingPair> getCoinGeckoData() {
        List<TradingPair> pairs = new ArrayList<>();
        
        // Проверяем Rate Limiter перед запросом
        if (!rateLimiterService.canMakeRequest("coingecko")) {
            System.out.println("Rate limit exceeded for coingecko request");
            return new ArrayList<>();
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false");
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.isArray()) {
                        for (JsonNode coinNode : jsonNode) {
                            TradingPair pair = parseCoinGeckoPair(coinNode);
                            if (pair != null) {
                                pairs.add(pair);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching CoinGecko data: " + e.getMessage());
        }
        
        return pairs;
    }
    
    /**
     * Parse Binance trading pair
     */
    private TradingPair parseBinancePair(JsonNode node) {
        try {
            TradingPair pair = new TradingPair();
            pair.setSymbol(node.get("symbol").asText());
            pair.setPrice(new BigDecimal(node.get("lastPrice").asText()));
            pair.setVolume24h(new BigDecimal(node.get("volume").asText()));
            pair.setPriceChange24h(new BigDecimal(node.get("priceChangePercent").asText()));
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            // Extract base and quote assets
            String symbol = pair.getSymbol();
            if (symbol.endsWith("USDT")) {
                pair.setBaseAsset(symbol.substring(0, symbol.length() - 4));
                pair.setQuoteAsset("USDT");
            }
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing Binance pair: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse CoinGecko trading pair
     */
    private TradingPair parseCoinGeckoPair(JsonNode node) {
        try {
            TradingPair pair = new TradingPair();
            String symbol = node.get("symbol").asText().toUpperCase() + "USDT";
            pair.setSymbol(symbol);
            pair.setPrice(new BigDecimal(node.get("current_price").asText()));
            pair.setVolume24h(new BigDecimal(node.get("total_volume").asText()));
            pair.setPriceChange24h(new BigDecimal(node.get("price_change_percentage_24h").asText()));
            pair.setIsActive(true);
            pair.setLastUpdated(LocalDateTime.now());
            
            // Extract base and quote assets
            pair.setBaseAsset(node.get("symbol").asText().toUpperCase());
            pair.setQuoteAsset("USDT");
            
            return pair;
        } catch (Exception e) {
            System.err.println("Error parsing CoinGecko pair: " + e.getMessage());
            return null;
        }
    }
}
