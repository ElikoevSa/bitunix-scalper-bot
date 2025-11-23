package com.bitunix.scalper.scheduler;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.model.TradingSignal;
import com.bitunix.scalper.repository.TradingSignalRepository;
import com.bitunix.scalper.service.BitunixApiService;
import com.bitunix.scalper.service.TechnicalAnalysisService;
import com.bitunix.scalper.service.TradingService;
import com.bitunix.scalper.service.TradingConfigService;
import com.bitunix.scalper.service.StrategyEvaluationService;
import com.bitunix.scalper.service.BybitDemoTradingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class TradingScheduler {
    
    @Autowired
    private BitunixApiService bitunixApiService;
    
    @Autowired
    private TradingService tradingService;
    
    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;
    
    @Autowired
    private List<TradingStrategyInterface> strategies;
    
    @Autowired
    private TradingConfigService configService;
    
    @Autowired
    private StrategyEvaluationService strategyEvaluationService;
    
    @Autowired
    private BybitDemoTradingService bybitDemoTradingService;
    
    @Autowired
    private TradingSignalRepository signalRepository;
    
    // Store active trades
    private final ConcurrentMap<String, Trade> activeTrades = new ConcurrentHashMap<>();
    
    // Trading configuration
    private boolean tradingEnabled = false;
    private double availableBalance = 10000.0; // Starting balance (will be updated from API)
    
    /**
     * Main trading loop - runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void executeTradingCycle() {
        if (!tradingEnabled) {
            return;
        }
        
        try {
            // Update balance from API
            updateBalanceFromAPI();
            
            // Get selected pairs from configuration
            List<String> selectedPairs = configService.getSelectedPairs();
            
            // Get trading pairs - only selected ones if configured, otherwise all
            List<TradingPair> allPairs;
            if (!selectedPairs.isEmpty()) {
                // Get only selected pairs from API
                allPairs = bitunixApiService.getTradingPairs(selectedPairs);
            } else {
                // Get all pairs if no selection
                allPairs = bitunixApiService.getAllTradingPairs();
            }
            
            // Filter pairs based on volume and active status
            List<TradingPair> activePairs = allPairs.stream()
                    .filter(pair -> pair.getIsActive() != null && pair.getIsActive())
                    .filter(pair -> pair.getVolume24h() != null && 
                                   pair.getVolume24h().doubleValue() > 1000)
                    .collect(Collectors.toList());
            
            // Update technical indicators
            for (TradingPair pair : activePairs) {
                List<TradingPair> historicalData = bitunixApiService.getKlineData(
                    pair.getSymbol(), "1m", 100);
                technicalAnalysisService.updateTechnicalIndicators(pair, historicalData);
            }
            
            // Check for exit signals on active trades
            checkExitSignals(activePairs);
            
            // Check for new entry signals
            checkEntrySignals(activePairs);
            
        } catch (Exception e) {
            System.err.println("Error in trading cycle: " + e.getMessage());
        }
    }
    
    /**
     * Check for exit signals on active trades
     */
    private void checkExitSignals(List<TradingPair> activePairs) {
        for (Trade trade : activeTrades.values()) {
            if (trade.getStatus() != Trade.TradeStatus.OPEN) {
                continue;
            }
            
            // Find current pair data
            TradingPair currentPair = activePairs.stream()
                    .filter(pair -> pair.getSymbol().equals(trade.getSymbol()))
                    .findFirst()
                    .orElse(null);
            
            if (currentPair == null) {
                continue;
            }
            
            // Find strategy for this trade
            TradingStrategyInterface strategy = strategies.stream()
                    .filter(s -> s.getName().equals(trade.getStrategy()))
                    .findFirst()
                    .orElse(null);
            
            if (strategy == null) {
                continue;
            }
            
            // Check exit signal
            if (strategy.shouldExit(trade, currentPair, null)) {
                Trade closedTrade = tradingService.closeTrade(trade, currentPair, strategy);
                activeTrades.remove(trade.getSymbol());
                System.out.println("Trade closed: " + closedTrade.getSymbol() + 
                                 " Profit: " + closedTrade.getProfit());
            }
        }
    }
    
    /**
     * Check for new entry signals
     * Analyzes all pairs and all strategies, then selects the best signal
     */
    private void checkEntrySignals(List<TradingPair> activePairs) {
        // Don't open new trades if we already have active trades
        if (!activeTrades.isEmpty()) {
            return;
        }
        
        // Get selected strategies from configuration
        List<TradingStrategyInterface> availableStrategies = strategies.stream()
                .filter(s -> s.isActive())
                .filter(s -> configService.isStrategySelected(s.getName()))
                .collect(Collectors.toList());
        
        if (availableStrategies.isEmpty()) {
            return;
        }
        
        // Analyze all pairs and all strategies to find the best signal
        TradingPair bestPair = null;
        TradingStrategyInterface bestStrategy = null;
        double bestScore = 0.0;
        List<TradingPair> bestHistoricalData = null;
        
        double minScore = configService.getActiveConfig().getMinStrategyScore();
        
        // Check each pair
        for (TradingPair pair : activePairs) {
            // Skip if already have active trade for this pair
            if (activeTrades.containsKey(pair.getSymbol())) {
                continue;
            }
            
            // Get historical data for technical analysis
            List<TradingPair> historicalData = bitunixApiService.getKlineData(
                pair.getSymbol(), "1m", 50);
            
            if (historicalData == null || historicalData.isEmpty()) {
                continue;
            }
            
            // Check each strategy for this pair
            for (TradingStrategyInterface strategy : availableStrategies) {
                if (!strategy.shouldEnter(pair, historicalData)) {
                    continue;
                }
                
                // Evaluate strategy score
                double score = strategyEvaluationService.evaluateStrategy(
                    pair, strategy, historicalData);
                
                // If score is above minimum and better than current best, update
                if (score >= minScore && score > bestScore) {
                    bestPair = pair;
                    bestStrategy = strategy;
                    bestScore = score;
                    bestHistoricalData = historicalData;
                }
            }
        }
        
        // If we found a good signal, execute it
        if (bestPair != null && bestStrategy != null && bestHistoricalData != null) {
            String signalReason = "Best signal selected: " + bestStrategy.getName() + 
                                 " for " + bestPair.getSymbol() + 
                                 " with score: " + String.format("%.2f", bestScore);
            
            // Save signal
            TradingSignal signal = new TradingSignal();
            signal.setSymbol(bestPair.getSymbol());
            signal.setStrategy(bestStrategy.getName());
            signal.setSignalType(TradingSignal.SignalType.BUY);
            signal.setPrice(bestPair.getPrice());
            signal.setSignalStrength(java.math.BigDecimal.valueOf(bestScore));
            signal.setSignalTime(java.time.LocalDateTime.now());
            signal.setReason(signalReason);
            signal.setExecuted(false);
            
            // Try to execute trade
            Trade newTrade = tradingService.executeTrade(bestPair, bestStrategy, 
                                                        bestHistoricalData, availableBalance);
            
            if (newTrade != null) {
                signal.setExecuted(true);
                signal.setExecutedTime(java.time.LocalDateTime.now());
                activeTrades.put(bestPair.getSymbol(), newTrade);
                System.out.println("New trade opened: " + newTrade.getSymbol() + 
                                 " Strategy: " + newTrade.getStrategy() +
                                 " Entry: " + newTrade.getEntryPrice() +
                                 " Score: " + String.format("%.2f", bestScore));
            } else {
                signal.setReason(signalReason + " (Trade not executed - check logs)");
            }
            
            signalRepository.save(signal);
        }
    }
    
    /**
     * Select the best trading pair based on technical analysis
     */
    private TradingPair selectBestTradingPair(List<TradingPair> pairs) {
        if (pairs.isEmpty()) {
            return null;
        }
        
        // Simple selection logic - prioritize pairs with good technical indicators
        return pairs.stream()
                .filter(pair -> pair.getRsi() != null)
                .filter(pair -> pair.getRsi().doubleValue() > 20 && pair.getRsi().doubleValue() < 80)
                .filter(pair -> pair.getVolume24h() != null && 
                               pair.getVolume24h().doubleValue() > 10000)
                .filter(pair -> !activeTrades.containsKey(pair.getSymbol()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Start trading
     */
    public void startTrading() {
        tradingEnabled = true;
        System.out.println("Trading started");
    }
    
    /**
     * Stop trading
     */
    public void stopTrading() {
        tradingEnabled = false;
        System.out.println("Trading stopped");
    }
    
    /**
     * Get trading status
     */
    public boolean isTradingEnabled() {
        return tradingEnabled;
    }
    
    /**
     * Get active trades
     */
    public ConcurrentMap<String, Trade> getActiveTrades() {
        return activeTrades;
    }
    
    /**
     * Get available balance
     */
    public double getAvailableBalance() {
        return availableBalance;
    }
    
    /**
     * Update available balance
     */
    public void updateBalance(double newBalance) {
        this.availableBalance = newBalance;
    }
    
    /**
     * Update balance from Bybit API
     */
    private void updateBalanceFromAPI() {
        try {
            JsonNode walletBalance = bybitDemoTradingService.getWalletBalance("UNIFIED");
            if (walletBalance != null && walletBalance.has("result") && walletBalance.get("result").has("list")) {
                JsonNode list = walletBalance.get("result").get("list");
                if (list.isArray() && list.size() > 0) {
                    JsonNode account = list.get(0);
                    // Try to get available balance (USDT)
                    if (account.has("coin")) {
                        JsonNode coins = account.get("coin");
                        if (coins.isArray()) {
                            for (JsonNode coin : coins) {
                                if (coin.has("coin") && coin.get("coin").asText().equals("USDT")) {
                                    if (coin.has("availableToWithdraw")) {
                                        String availableStr = coin.get("availableToWithdraw").asText();
                                        if (availableStr != null && !availableStr.isEmpty()) {
                                            this.availableBalance = Double.parseDouble(availableStr);
                                            return;
                                        }
                                    }
                                    // Fallback to walletBalance
                                    if (coin.has("walletBalance")) {
                                        String balanceStr = coin.get("walletBalance").asText();
                                        if (balanceStr != null && !balanceStr.isEmpty()) {
                                            this.availableBalance = Double.parseDouble(balanceStr);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Fallback to totalEquity
                    if (account.has("totalEquity")) {
                        String totalEquityStr = account.get("totalEquity").asText();
                        if (totalEquityStr != null && !totalEquityStr.isEmpty()) {
                            this.availableBalance = Double.parseDouble(totalEquityStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating balance from API: " + e.getMessage());
            // Keep current balance if API call fails
        }
    }
}
