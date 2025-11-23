package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BollingerBounceStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "Bollinger Bounce";
    private static final int PRIORITY = 4;
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getBollingerUpper() == null || pair.getBollingerLower() == null || 
            pair.getPrice() == null || historicalData.size() < 20) {
            return false;
        }
        
        BigDecimal price = pair.getPrice();
        BigDecimal upperBand = pair.getBollingerUpper();
        BigDecimal lowerBand = pair.getBollingerLower();
        
        // Enter long when price touches lower band
        if (price.compareTo(lowerBand) <= 0) {
            return true;
        }
        
        // Enter short when price touches upper band
        if (price.compareTo(upperBand) >= 0) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getBollingerUpper() == null || currentPair.getBollingerLower() == null || 
            currentPair.getPrice() == null) {
            return false;
        }
        
        BigDecimal price = currentPair.getPrice();
        BigDecimal upperBand = currentPair.getBollingerUpper();
        BigDecimal lowerBand = currentPair.getBollingerLower();
        
        // Exit long when price reaches middle of bands
        if (trade.getType() == Trade.TradeType.BUY && 
            price.compareTo(lowerBand.add(upperBand).divide(BigDecimal.valueOf(2))) >= 0) {
            return true;
        }
        
        // Exit short when price reaches middle of bands
        if (trade.getType() == Trade.TradeType.SELL && 
            price.compareTo(lowerBand.add(upperBand).divide(BigDecimal.valueOf(2))) <= 0) {
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
        // Risk 1.5% of available balance per trade
        return availableBalance * 0.015;
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
