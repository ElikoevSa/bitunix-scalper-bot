package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VolumeSpikeStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "Volume Spike";
    private static final int PRIORITY = 7;
    private static final double VOLUME_SPIKE_THRESHOLD = 2.0; // 200% of average volume
    
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
        
        // Check for significant volume spike
        if (volumeRatio.doubleValue() >= VOLUME_SPIKE_THRESHOLD) {
            // Check for price movement in same direction as volume
            if (historicalData.size() >= 2) {
                TradingPair previousPair = historicalData.get(historicalData.size() - 2);
                if (previousPair.getPrice() != null) {
                    // Price should be moving up with volume spike
                    if (pair.getPrice().compareTo(previousPair.getPrice()) > 0) {
                        return true;
                    }
                    // Price should be moving down with volume spike
                    if (pair.getPrice().compareTo(previousPair.getPrice()) < 0) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getVolume24h() == null || historicalData.size() < 5) {
            return false;
        }
        
        // Calculate average volume over last 5 periods
        BigDecimal avgVolume = calculateAverageVolume(historicalData, 5);
        if (avgVolume == null || avgVolume.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        
        BigDecimal currentVolume = currentPair.getVolume24h();
        BigDecimal volumeRatio = currentVolume.divide(avgVolume, 4, BigDecimal.ROUND_HALF_UP);
        
        // Exit when volume drops significantly
        if (volumeRatio.doubleValue() < 0.5) {
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
        // Risk 2.5% of available balance per trade for volume spike
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
