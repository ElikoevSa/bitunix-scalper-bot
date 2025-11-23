package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.model.Trade;

import java.util.List;

public interface TradingStrategyInterface {
    
    /**
     * Get strategy name
     */
    String getName();
    
    /**
     * Check if strategy should enter a trade
     */
    boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData);
    
    /**
     * Check if strategy should exit a trade
     */
    boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData);
    
    /**
     * Calculate entry price
     */
    double calculateEntryPrice(TradingPair pair);
    
    /**
     * Calculate exit price
     */
    double calculateExitPrice(Trade trade, TradingPair currentPair);
    
    /**
     * Calculate position size
     */
    double calculatePositionSize(TradingPair pair, double availableBalance);
    
    /**
     * Get strategy priority (higher number = higher priority)
     */
    int getPriority();
    
    /**
     * Check if strategy is active
     */
    boolean isActive();
}
