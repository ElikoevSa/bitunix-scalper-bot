package com.bitunix.scalper.controller;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.model.TradingSignal;
import com.bitunix.scalper.repository.TradeRepository;
import com.bitunix.scalper.repository.TradingSignalRepository;
import com.bitunix.scalper.scheduler.TradingScheduler;
import com.bitunix.scalper.service.BitunixApiService;
import com.bitunix.scalper.service.TradingService;
import com.bitunix.scalper.service.RateLimiterService;
import com.bitunix.scalper.service.TradingConfigService;
import com.bitunix.scalper.service.BybitDemoTradingService;
import com.bitunix.scalper.service.BalanceCacheService;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
    private TradingScheduler tradingScheduler;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Autowired
    private TradingConfigService configService;
    
    @Autowired
    private BybitDemoTradingService bybitDemoTradingService;
    
    @Autowired
    private BalanceCacheService balanceCacheService;
    
    @Autowired
    private TradingSignalRepository signalRepository;
    
    @Autowired
    private List<TradingStrategyInterface> strategies;
    
    @GetMapping("/")
    public String dashboard(Model model) {
        // Get selected pairs from configuration
        List<String> selectedPairs = configService.getSelectedPairs();
        
        // Get trading pairs - only selected ones if configured, otherwise all
        // Use demo data by default for fast loading, try to get real data if available
        List<TradingPair> allPairs = getDemoTradingPairs();
        
        try {
            if (!selectedPairs.isEmpty()) {
                // Get only selected pairs from API (non-blocking)
                List<TradingPair> apiPairs = bitunixApiService.getTradingPairs(selectedPairs);
                if (apiPairs != null && !apiPairs.isEmpty()) {
                    allPairs = apiPairs;
                }
            } else {
                // Get all pairs if no selection (non-blocking)
                List<TradingPair> apiPairs = bitunixApiService.getAllTradingPairs();
                if (apiPairs != null && !apiPairs.isEmpty()) {
                    allPairs = apiPairs;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching trading pairs: " + e.getMessage());
            // Continue with demo data
        }
        
        // Filter active pairs with sufficient volume and limit to 20 pairs for performance
        List<TradingPair> activePairs = allPairs.stream()
                .filter(pair -> pair.getIsActive() != null && pair.getIsActive())
                .filter(pair -> pair.getVolume24h() != null && 
                               pair.getVolume24h().compareTo(BigDecimal.valueOf(1000)) > 0)
                .limit(20) // Limit to 20 pairs for better performance
                .collect(Collectors.toList());
        
        // Skip technical indicators update for faster loading
        // Technical indicators are updated in trading cycle, not in dashboard
        // This prevents blocking the dashboard with API calls
        
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
        
        // Filter pairs to show only selected ones in the table
        List<TradingPair> displayPairs;
        if (!selectedPairs.isEmpty()) {
            // Show only selected pairs
            displayPairs = activePairs.stream()
                    .filter(pair -> selectedPairs.contains(pair.getSymbol()))
                    .collect(Collectors.toList());
        } else {
            // Show all pairs if none selected
            displayPairs = activePairs;
        }
        
        model.addAttribute("activePairs", displayPairs);
        model.addAttribute("selectedPairs", selectedPairs); // List of selected pair symbols
        model.addAttribute("strategies", activeStrategies);
        model.addAttribute("tradingEnabled", tradingScheduler.isTradingEnabled());
        model.addAttribute("activeTradesCount", tradingScheduler.getActiveTrades().size());
        
        // Rate Limiter information
        model.addAttribute("bitunixRequests", rateLimiterService.getCurrentRequestCount("bitunix"));
        model.addAttribute("binanceRequests", rateLimiterService.getCurrentRequestCount("binance"));
        model.addAttribute("coingeckoRequests", rateLimiterService.getCurrentRequestCount("coingecko"));
        model.addAttribute("tradingCycleRequests", rateLimiterService.getCurrentRequestCount("trading_cycle"));
        
        // Get account balance from cache or API (with 10 minute limit)
        BigDecimal totalBalance = null;
        Map<String, BigDecimal> coinBalances = new HashMap<>();
        
        // Try to get from cache first
        if (!balanceCacheService.shouldFetchBalance()) {
            totalBalance = balanceCacheService.getCachedTotalBalance();
            coinBalances = balanceCacheService.getCachedCoinBalances();
            System.out.println("Using cached balance");
        }
        
        // If cache expired or empty, try to fetch from API
        if (totalBalance == null && balanceCacheService.shouldFetchBalance()) {
            try {
                // Check if we can make request without waiting
                if (rateLimiterService.canMakeRequest("bybit_demo")) {
                    JsonNode walletBalance = bybitDemoTradingService.getWalletBalance("UNIFIED");
                    if (walletBalance != null) {
                        model.addAttribute("walletBalance", walletBalance);
                        
                        // Parse and extract balance information
                        totalBalance = parseTotalBalance(walletBalance);
                        coinBalances = parseCoinBalances(walletBalance);
                        
                        // Update cache
                        balanceCacheService.updateCache(walletBalance, totalBalance, coinBalances);
                        System.out.println("Balance updated from API and cached");
                    }
                } else {
                    System.out.println("Rate limit exceeded for balance request");
                }
            } catch (Exception e) {
                System.err.println("Error fetching wallet balance: " + e.getMessage());
            }
        }
        
        // Use fallback if still no balance
        if (totalBalance == null) {
            // Try cached balance even if expired
            totalBalance = balanceCacheService.getCachedTotalBalance();
            coinBalances = balanceCacheService.getCachedCoinBalances();
            
            // Final fallback to scheduler balance
            if (totalBalance == null) {
                double schedulerBalance = tradingScheduler.getAvailableBalance();
                totalBalance = BigDecimal.valueOf(schedulerBalance);
            }
        }
        
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("coinBalances", coinBalances);
        
        // Get recent trading signals (last 20)
        List<TradingSignal> recentSignals = signalRepository.findTop10ByOrderBySignalTimeDesc();
        model.addAttribute("recentSignals", recentSignals);
        
        // Get unexecuted signals count
        long unexecutedSignals = recentSignals.stream()
                .filter(s -> !s.getExecuted())
                .count();
        model.addAttribute("unexecutedSignalsCount", unexecutedSignals);
        
        return "dashboard";
    }
    
    @PostMapping("/start-trading")
    public String startTrading(@RequestParam(required = false) String symbol, 
                              @RequestParam(required = false) String strategyName, 
                              Model model) {
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
    
    /**
     * Parse total balance from wallet balance JSON
     */
    private BigDecimal parseTotalBalance(JsonNode walletBalance) {
        try {
            if (walletBalance.has("result") && walletBalance.get("result").has("list")) {
                JsonNode list = walletBalance.get("result").get("list");
                if (list.isArray() && list.size() > 0) {
                    JsonNode account = list.get(0);
                    if (account.has("totalEquity")) {
                        String totalEquityStr = account.get("totalEquity").asText();
                        if (totalEquityStr != null && !totalEquityStr.isEmpty()) {
                            return new BigDecimal(totalEquityStr);
                        }
                    }
                    // Fallback to totalWalletBalance
                    if (account.has("totalWalletBalance")) {
                        String totalWalletBalanceStr = account.get("totalWalletBalance").asText();
                        if (totalWalletBalanceStr != null && !totalWalletBalanceStr.isEmpty()) {
                            return new BigDecimal(totalWalletBalanceStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing total balance: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Parse individual coin balances from wallet balance JSON
     */
    private Map<String, BigDecimal> parseCoinBalances(JsonNode walletBalance) {
        Map<String, BigDecimal> balances = new HashMap<>();
        
        try {
            if (walletBalance.has("result") && walletBalance.get("result").has("list")) {
                JsonNode list = walletBalance.get("result").get("list");
                if (list.isArray() && list.size() > 0) {
                    JsonNode account = list.get(0);
                    if (account.has("coin")) {
                        JsonNode coins = account.get("coin");
                        if (coins.isArray()) {
                            for (JsonNode coin : coins) {
                                if (coin.has("coin") && coin.has("walletBalance")) {
                                    String coinName = coin.get("coin").asText();
                                    String balanceStr = coin.get("walletBalance").asText();
                                    if (balanceStr != null && !balanceStr.isEmpty()) {
                                        try {
                                            BigDecimal balance = new BigDecimal(balanceStr);
                                            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                                                balances.put(coinName, balance);
                                            }
                                        } catch (Exception e) {
                                            // Skip invalid balance
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing coin balances: " + e.getMessage());
        }
        
        return balances;
    }
}
