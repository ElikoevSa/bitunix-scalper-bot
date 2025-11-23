package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SupportResistanceStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "Support/Resistance";
    private static final int PRIORITY = 2;
    private static final double TOUCH_TOLERANCE = 0.001; // 0.1% tolerance
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getPrice() == null || pair.getSupportLevel() == null || 
            pair.getResistanceLevel() == null || historicalData.size() < 50) {
            return false;
        }
        
        BigDecimal price = pair.getPrice();
        BigDecimal support = pair.getSupportLevel();
        BigDecimal resistance = pair.getResistanceLevel();
        
        // Enter long near support level
        if (isNearLevel(price, support)) {
            return true;
        }
        
        // Enter short near resistance level
        if (isNearLevel(price, resistance)) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getPrice() == null || currentPair.getSupportLevel() == null || 
            currentPair.getResistanceLevel() == null) {
            return false;
        }
        
        BigDecimal price = currentPair.getPrice();
        BigDecimal support = currentPair.getSupportLevel();
        BigDecimal resistance = currentPair.getResistanceLevel();
        
        // Exit long when price reaches resistance
        if (trade.getType() == Trade.TradeType.BUY && isNearLevel(price, resistance)) {
            return true;
        }
        
        // Exit short when price reaches support
        if (trade.getType() == Trade.TradeType.SELL && isNearLevel(price, support)) {
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
        // Risk 1% of available balance per trade for support/resistance
        return availableBalance * 0.01;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean isActive() {
        return true;
    }
    
    private boolean isNearLevel(BigDecimal price, BigDecimal level) {
        if (price == null || level == null || level.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        
        BigDecimal difference = price.subtract(level).abs();
        BigDecimal tolerance = level.multiply(BigDecimal.valueOf(TOUCH_TOLERANCE));
        
        return difference.compareTo(tolerance) <= 0;
    }
}
