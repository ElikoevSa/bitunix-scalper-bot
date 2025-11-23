package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MeanReversionStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "Mean Reversion";
    private static final int PRIORITY = 1;
    private static final double DEVIATION_THRESHOLD = 2.0; // 2 standard deviations
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getPrice() == null || historicalData.size() < 20) {
            return false;
        }
        
        // Calculate moving average and standard deviation
        BigDecimal[] stats = calculateMeanAndStdDev(historicalData, 20);
        if (stats == null) {
            return false;
        }
        
        BigDecimal mean = stats[0];
        BigDecimal stdDev = stats[1];
        BigDecimal price = pair.getPrice();
        
        // Calculate z-score
        BigDecimal zScore = price.subtract(mean).divide(stdDev, 4, BigDecimal.ROUND_HALF_UP);
        
        // Enter long when price is significantly below mean (oversold)
        if (zScore.doubleValue() < -DEVIATION_THRESHOLD) {
            return true;
        }
        
        // Enter short when price is significantly above mean (overbought)
        if (zScore.doubleValue() > DEVIATION_THRESHOLD) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getPrice() == null || historicalData.size() < 20) {
            return false;
        }
        
        // Calculate moving average and standard deviation
        BigDecimal[] stats = calculateMeanAndStdDev(historicalData, 20);
        if (stats == null) {
            return false;
        }
        
        BigDecimal mean = stats[0];
        BigDecimal stdDev = stats[1];
        BigDecimal price = currentPair.getPrice();
        
        // Calculate z-score
        BigDecimal zScore = price.subtract(mean).divide(stdDev, 4, BigDecimal.ROUND_HALF_UP);
        
        // Exit long when price returns to mean
        if (trade.getType() == Trade.TradeType.BUY && zScore.doubleValue() >= 0) {
            return true;
        }
        
        // Exit short when price returns to mean
        if (trade.getType() == Trade.TradeType.SELL && zScore.doubleValue() <= 0) {
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
        // Risk 1.5% of available balance per trade for mean reversion
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
    
    private BigDecimal[] calculateMeanAndStdDev(List<TradingPair> data, int periods) {
        if (data.size() < periods) {
            return null;
        }
        
        // Calculate mean
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (int i = data.size() - periods; i < data.size(); i++) {
            if (data.get(i).getPrice() != null) {
                sum = sum.add(data.get(i).getPrice());
                count++;
            }
        }
        
        if (count == 0) {
            return null;
        }
        
        BigDecimal mean = sum.divide(BigDecimal.valueOf(count), 8, BigDecimal.ROUND_HALF_UP);
        
        // Calculate standard deviation
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = data.size() - periods; i < data.size(); i++) {
            if (data.get(i).getPrice() != null) {
                BigDecimal diff = data.get(i).getPrice().subtract(mean);
                variance = variance.add(diff.multiply(diff));
            }
        }
        
        variance = variance.divide(BigDecimal.valueOf(count), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal stdDev = new BigDecimal(Math.sqrt(variance.doubleValue()));
        
        return new BigDecimal[]{mean, stdDev};
    }
}
