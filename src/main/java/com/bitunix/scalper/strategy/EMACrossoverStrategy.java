package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class EMACrossoverStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "EMA Crossover";
    private static final int PRIORITY = 3;
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getEma12() == null || pair.getEma26() == null || historicalData.size() < 26) {
            return false;
        }
        
        BigDecimal ema12 = pair.getEma12();
        BigDecimal ema26 = pair.getEma26();
        
        // Check for golden cross (EMA12 > EMA26)
        if (ema12.compareTo(ema26) > 0) {
            // Check if previous candle had bearish crossover
            if (historicalData.size() >= 2) {
                TradingPair previousPair = historicalData.get(historicalData.size() - 2);
                if (previousPair.getEma12() != null && previousPair.getEma26() != null) {
                    if (previousPair.getEma12().compareTo(previousPair.getEma26()) <= 0) {
                        return true; // Golden cross detected
                    }
                }
            }
        }
        
        // Check for death cross (EMA12 < EMA26)
        if (ema12.compareTo(ema26) < 0) {
            // Check if previous candle had bullish crossover
            if (historicalData.size() >= 2) {
                TradingPair previousPair = historicalData.get(historicalData.size() - 2);
                if (previousPair.getEma12() != null && previousPair.getEma26() != null) {
                    if (previousPair.getEma12().compareTo(previousPair.getEma26()) >= 0) {
                        return true; // Death cross detected
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getEma12() == null || currentPair.getEma26() == null) {
            return false;
        }
        
        BigDecimal ema12 = currentPair.getEma12();
        BigDecimal ema26 = currentPair.getEma26();
        
        // Exit long position on death cross
        if (trade.getType() == Trade.TradeType.BUY && ema12.compareTo(ema26) < 0) {
            return true;
        }
        
        // Exit short position on golden cross
        if (trade.getType() == Trade.TradeType.SELL && ema12.compareTo(ema26) > 0) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public double calculateEntryPrice(TradingPair pair) {
        return pair.getPrice().doubleValue();
    }
    
    @Override
    public double calculateExitPrice(Trade trade, TradingPair currentPair) {
        return currentPair.getPrice().doubleValue();
    }
    
    @Override
    public double calculatePositionSize(TradingPair pair, double availableBalance) {
        // Risk 2.5% of available balance per trade
        return availableBalance * 0.025;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean isActive() {
        return true;
    }
}
