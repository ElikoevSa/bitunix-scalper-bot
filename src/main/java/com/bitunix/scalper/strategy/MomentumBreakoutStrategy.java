package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MomentumBreakoutStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "Momentum Breakout";
    private static final int PRIORITY = 6;
    private static final double VOLUME_THRESHOLD = 1.5; // 150% of average volume
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getVolume24h() == null || historicalData.size() < 20) {
            return false;
        }
        
        // Calculate average volume over last 20 periods
        BigDecimal avgVolume = calculateAverageVolume(historicalData, 20);
        if (avgVolume == null || avgVolume.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        
        BigDecimal currentVolume = pair.getVolume24h();
        BigDecimal volumeRatio = currentVolume.divide(avgVolume, 4, BigDecimal.ROUND_HALF_UP);
        
        // Check for volume spike
        if (volumeRatio.doubleValue() >= VOLUME_THRESHOLD) {
            // Check for price momentum
            if (historicalData.size() >= 3) {
                TradingPair prev1 = historicalData.get(historicalData.size() - 2);
                TradingPair prev2 = historicalData.get(historicalData.size() - 3);
                
                if (prev1.getPrice() != null && prev2.getPrice() != null) {
                    // Bullish momentum
                    if (pair.getPrice().compareTo(prev1.getPrice()) > 0 && 
                        prev1.getPrice().compareTo(prev2.getPrice()) > 0) {
                        return true;
                    }
                    
                    // Bearish momentum
                    if (pair.getPrice().compareTo(prev1.getPrice()) < 0 && 
                        prev1.getPrice().compareTo(prev2.getPrice()) < 0) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getVolume24h() == null || historicalData.size() < 10) {
            return false;
        }
        
        // Calculate average volume over last 10 periods
        BigDecimal avgVolume = calculateAverageVolume(historicalData, 10);
        if (avgVolume == null || avgVolume.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        
        BigDecimal currentVolume = currentPair.getVolume24h();
        BigDecimal volumeRatio = currentVolume.divide(avgVolume, 4, BigDecimal.ROUND_HALF_UP);
        
        // Exit when volume drops below threshold
        if (volumeRatio.doubleValue() < 0.8) {
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
        // Risk 3% of available balance per trade for momentum strategy
        return availableBalance * 0.03;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean isActive() {
        return true;
    }
    
    private BigDecimal calculateAverageVolume(List<TradingPair> data, int periods) {
        if (data.size() < periods) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (int i = data.size() - periods; i < data.size(); i++) {
            if (data.get(i).getVolume24h() != null) {
                sum = sum.add(data.get(i).getVolume24h());
                count++;
            }
        }
        
        if (count == 0) {
            return null;
        }
        
        return sum.divide(BigDecimal.valueOf(count), 8, BigDecimal.ROUND_HALF_UP);
    }
}
