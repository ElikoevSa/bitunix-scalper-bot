package com.bitunix.scalper.scheduler;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.service.BitunixApiService;
import com.bitunix.scalper.service.TechnicalAnalysisService;
import com.bitunix.scalper.service.TradingService;
import com.bitunix.scalper.service.RateLimiterService;
import com.bitunix.scalper.service.TradingConfigService;
import com.bitunix.scalper.service.StrategyEvaluationService;
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
    private RateLimiterService rateLimiterService;
    
    @Autowired
    private TradingConfigService configService;
    
    @Autowired
    private StrategyEvaluationService strategyEvaluationService;
    
    // Store active trades
    private final ConcurrentMap<String, Trade> activeTrades = new ConcurrentHashMap<>();
    
    // Trading configuration
    private boolean tradingEnabled = false;
    private double availableBalance = 10000.0; // Starting balance
    
    /**
     * Main trading loop - runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void executeTradingCycle() {
        if (!tradingEnabled) {
            return;
        }
        
        try {
            // Проверяем Rate Limiter перед получением данных
            if (!rateLimiterService.canMakeRequest("trading_cycle")) {
                System.out.println("Rate limit exceeded for trading cycle, skipping this iteration");
                return;
            }
            
            // Get all trading pairs
            List<TradingPair> allPairs = bitunixApiService.getAllTradingPairs();
            
            // Filter pairs based on configuration
            List<TradingPair> activePairs = allPairs.stream()
                    .filter(pair -> pair.getIsActive() != null && pair.getIsActive())
                    .filter(pair -> pair.getVolume24h() != null && 
                                   pair.getVolume24h().doubleValue() > 1000)
                    .filter(pair -> configService.isPairSelected(pair.getSymbol()))
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
     */
    private void checkEntrySignals(List<TradingPair> activePairs) {
        // Don't open new trades if we already have active trades
        if (!activeTrades.isEmpty()) {
            return;
        }
        
        // Find best trading pair
        TradingPair bestPair = selectBestTradingPair(activePairs);
        if (bestPair == null) {
            return;
        }
        
        // Get historical data for technical analysis
        List<TradingPair> historicalData = bitunixApiService.getKlineData(
            bestPair.getSymbol(), "1m", 50);
        
        // Get selected strategies from configuration
        List<TradingStrategyInterface> availableStrategies = strategies.stream()
                .filter(s -> s.isActive())
                .filter(s -> configService.isStrategySelected(s.getName()))
                .collect(Collectors.toList());
        
        if (availableStrategies.isEmpty()) {
            return;
        }
        
        // Auto-select best strategy if enabled
        TradingStrategyInterface selectedStrategy = null;
        if (configService.getActiveConfig().getAutoSelectBestStrategy()) {
            double minScore = configService.getActiveConfig().getMinStrategyScore();
            selectedStrategy = strategyEvaluationService.findBestStrategy(
                bestPair, availableStrategies, historicalData, minScore);
        } else {
            // Use first strategy that signals entry
            for (TradingStrategyInterface strategy : availableStrategies) {
                if (strategy.shouldEnter(bestPair, historicalData)) {
                    selectedStrategy = strategy;
                    break;
                }
            }
        }
        
        if (selectedStrategy != null && selectedStrategy.shouldEnter(bestPair, historicalData)) {
            Trade newTrade = tradingService.executeTrade(bestPair, selectedStrategy, 
                                                        historicalData, availableBalance);
            
            if (newTrade != null) {
                activeTrades.put(bestPair.getSymbol(), newTrade);
                System.out.println("New trade opened: " + newTrade.getSymbol() + 
                                 " Strategy: " + newTrade.getStrategy() +
                                 " Entry: " + newTrade.getEntryPrice());
            }
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
}
