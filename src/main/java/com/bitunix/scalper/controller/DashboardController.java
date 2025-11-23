package com.bitunix.scalper.controller;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.repository.TradeRepository;
import com.bitunix.scalper.scheduler.TradingScheduler;
import com.bitunix.scalper.service.BitunixApiService;
import com.bitunix.scalper.service.TradingService;
import com.bitunix.scalper.service.TechnicalAnalysisService;
import com.bitunix.scalper.service.RateLimiterService;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    
    @Autowired
    private BitunixApiService bitunixApiService;
    
    @Autowired
    private TradingService tradingService;
    
    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;
    
    @Autowired
    private TradingScheduler tradingScheduler;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Autowired
    private List<TradingStrategyInterface> strategies;
    
    @GetMapping("/")
    public String dashboard(Model model) {
        // Проверяем Rate Limiter перед загрузкой дашборда
        if (!rateLimiterService.canMakeRequest("dashboard")) {
            System.out.println("Rate limit exceeded for dashboard, using cached data");
            // Можно добавить логику для показа кэшированных данных
        }
        
        // Get all trading pairs with error handling
        List<TradingPair> allPairs;
        try {
            allPairs = bitunixApiService.getAllTradingPairs();
            if (allPairs == null || allPairs.isEmpty()) {
                System.out.println("No trading pairs received, using demo data");
                allPairs = getDemoTradingPairs();
            }
        } catch (Exception e) {
            System.err.println("Error fetching trading pairs: " + e.getMessage());
            System.out.println("Using demo data due to API error");
            allPairs = getDemoTradingPairs();
        }
        
        // Filter active pairs with sufficient volume and limit to 20 pairs for performance
        List<TradingPair> activePairs = allPairs.stream()
                .filter(pair -> pair.getIsActive() != null && pair.getIsActive())
                .filter(pair -> pair.getVolume24h() != null && 
                               pair.getVolume24h().compareTo(BigDecimal.valueOf(1000)) > 0)
                .limit(20) // Limit to 20 pairs for better performance
                .collect(Collectors.toList());
        
        // Update technical indicators only for first 5 pairs to avoid API overload
        List<TradingPair> limitedPairs = activePairs.stream()
                .limit(5)
                .collect(Collectors.toList());
        
        for (TradingPair pair : limitedPairs) {
            try {
                // Skip technical analysis for demo data
                if (pair.getRsi() != null && pair.getBollingerUpper() != null) {
                    continue; // Already has technical indicators (demo data)
                }
                
                List<TradingPair> historicalData = bitunixApiService.getKlineData(
                    pair.getSymbol(), "1m", 100);
                if (historicalData != null && !historicalData.isEmpty()) {
                    technicalAnalysisService.updateTechnicalIndicators(pair, historicalData);
                }
            } catch (Exception e) {
                System.err.println("Error updating technical indicators for " + pair.getSymbol() + ": " + e.getMessage());
                // Continue with next pair
            }
        }
        
        // Get best trading pair based on technical analysis
        TradingPair selectedPair = selectBestTradingPair(activePairs);
        
        // If no pair selected, use first available pair for demo
        if (selectedPair == null && !activePairs.isEmpty()) {
            selectedPair = activePairs.get(0);
        }
        
        // Get active strategies
        List<TradingStrategyInterface> activeStrategies = strategies.stream()
                .filter(TradingStrategyInterface::isActive)
                .collect(Collectors.toList());
        
        // Get current strategy (first active strategy for demo)
        TradingStrategyInterface currentStrategy = activeStrategies.isEmpty() ? 
                null : activeStrategies.get(0);
        
        // Ensure we have at least one strategy for demo
        if (currentStrategy == null && !strategies.isEmpty()) {
            currentStrategy = strategies.get(0);
        }
        
        // Get current active trade from scheduler
        Map<String, Trade> activeTrades = tradingScheduler.getActiveTrades();
        Trade currentTrade = activeTrades.isEmpty() ? null : activeTrades.values().iterator().next();
        
        // Calculate statistics from database
        List<Trade> allTrades = tradeRepository.findAll();
        BigDecimal totalProfit = tradingService.calculateTotalProfit(allTrades);
        BigDecimal successRate = tradingService.calculateSuccessRate(allTrades);
        int totalTrades = allTrades.size();
        int successfulTrades = (int) allTrades.stream()
                .filter(trade -> trade.getStatus() == Trade.TradeStatus.CLOSED)
                .filter(trade -> trade.getProfit() != null && trade.getProfit().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        model.addAttribute("selectedPair", selectedPair);
        model.addAttribute("currentStrategy", currentStrategy);
        model.addAttribute("currentTrade", currentTrade);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("successRate", successRate);
        model.addAttribute("totalTrades", totalTrades);
        model.addAttribute("successfulTrades", successfulTrades);
        model.addAttribute("activePairs", activePairs);
        model.addAttribute("strategies", activeStrategies);
        model.addAttribute("tradingEnabled", tradingScheduler.isTradingEnabled());
        model.addAttribute("activeTradesCount", tradingScheduler.getActiveTrades().size());
        
        // Rate Limiter information
        model.addAttribute("bitunixRequests", rateLimiterService.getCurrentRequestCount("bitunix"));
        model.addAttribute("binanceRequests", rateLimiterService.getCurrentRequestCount("binance"));
        model.addAttribute("coingeckoRequests", rateLimiterService.getCurrentRequestCount("coingecko"));
        model.addAttribute("tradingCycleRequests", rateLimiterService.getCurrentRequestCount("trading_cycle"));
        
        return "dashboard";
    }
    
    @PostMapping("/start-trading")
    public String startTrading(@RequestParam(required = false) String symbol, 
                              @RequestParam(required = false) String strategyName, 
                              Model model) {
        // Проверяем Rate Limiter перед запуском торговли
        if (!rateLimiterService.canMakeRequest("trading_control")) {
            System.out.println("Rate limit exceeded for trading control");
            return "redirect:/";
        }
        
        try {
            tradingScheduler.startTrading();
            System.out.println("Trading started successfully");
        } catch (Exception e) {
            System.err.println("Error starting trading: " + e.getMessage());
        }
        return "redirect:/";
    }
    
    @PostMapping("/stop-trading")
    public String stopTrading(Model model) {
        // Проверяем Rate Limiter перед остановкой торговли
        if (!rateLimiterService.canMakeRequest("trading_control")) {
            System.out.println("Rate limit exceeded for trading control");
            return "redirect:/";
        }
        
        try {
            tradingScheduler.stopTrading();
            System.out.println("Trading stopped successfully");
        } catch (Exception e) {
            System.err.println("Error stopping trading: " + e.getMessage());
        }
        return "redirect:/";
    }
    
    /**
     * Select the best trading pair based on technical analysis
     */
    private TradingPair selectBestTradingPair(List<TradingPair> pairs) {
        if (pairs.isEmpty()) {
            return null;
        }
        
        // Simple selection logic - in real implementation, this would be more sophisticated
        return pairs.stream()
                .filter(pair -> pair.getRsi() != null)
                .filter(pair -> pair.getRsi().doubleValue() > 20 && pair.getRsi().doubleValue() < 80)
                .filter(pair -> pair.getVolume24h() != null && 
                               pair.getVolume24h().compareTo(BigDecimal.valueOf(10000)) > 0)
                .findFirst()
                .orElse(pairs.get(0));
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
            pair.setLastUpdated(java.time.LocalDateTime.now());
            
            // Add some technical indicators
            pair.setRsi(BigDecimal.valueOf(30 + Math.random() * 40)); // RSI between 30-70
            pair.setBollingerUpper(BigDecimal.valueOf(prices[i] * 1.02));
            pair.setBollingerLower(BigDecimal.valueOf(prices[i] * 0.98));
            pair.setEma12(BigDecimal.valueOf(prices[i] * (0.99 + Math.random() * 0.02)));
            pair.setEma26(BigDecimal.valueOf(prices[i] * (0.98 + Math.random() * 0.04)));
            pair.setSupportLevel(BigDecimal.valueOf(prices[i] * 0.95));
            pair.setResistanceLevel(BigDecimal.valueOf(prices[i] * 1.05));
            
            // Ensure all required fields are set
            pair.setBaseAsset(symbols[i].substring(0, symbols[i].length() - 4)); // Remove USDT
            pair.setQuoteAsset("USDT");
            
            demoPairs.add(pair);
        }
        
        return demoPairs;
    }
}
