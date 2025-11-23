package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class TechnicalAnalysisService {
    
    @Autowired
    private TradingConfigService configService;
    
    /**
     * Calculate RSI (Relative Strength Index)
     */
    public BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) {
            return null;
        }
        
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }
        
        BigDecimal avgGain = calculateSMA(gains, period);
        BigDecimal avgLoss = calculateSMA(losses, period);
        
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
            BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP)
        );
        
        return rsi;
    }
    
    /**
     * Calculate Bollinger Bands
     */
    public BigDecimal[] calculateBollingerBands(List<BigDecimal> prices, int period, double stdDevMultiplier) {
        if (prices.size() < period) {
            return null;
        }
        
        BigDecimal sma = calculateSMA(prices, period);
        BigDecimal stdDev = calculateStandardDeviation(prices, period);
        
        BigDecimal upperBand = sma.add(stdDev.multiply(BigDecimal.valueOf(stdDevMultiplier)));
        BigDecimal lowerBand = sma.subtract(stdDev.multiply(BigDecimal.valueOf(stdDevMultiplier)));
        
        return new BigDecimal[]{upperBand, sma, lowerBand};
    }
    
    /**
     * Calculate EMA (Exponential Moving Average)
     */
    public BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return null;
        }
        
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = prices.get(0);
        
        for (int i = 1; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier).add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        
        return ema;
    }
    
    /**
     * Calculate Support and Resistance levels
     */
    public BigDecimal[] calculateSupportResistance(List<BigDecimal> prices, int lookbackPeriod) {
        if (prices.size() < lookbackPeriod) {
            return null;
        }
        
        List<BigDecimal> recentPrices = prices.subList(prices.size() - lookbackPeriod, prices.size());
        
        BigDecimal min = recentPrices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = recentPrices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        
        return new BigDecimal[]{min, max};
    }
    
    /**
     * Calculate Simple Moving Average
     */
    private BigDecimal calculateSMA(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum = sum.add(values.get(i));
        }
        
        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate Standard Deviation
     */
    private BigDecimal calculateStandardDeviation(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return null;
        }
        
        BigDecimal sma = calculateSMA(values, period);
        BigDecimal variance = BigDecimal.ZERO;
        
        for (int i = values.size() - period; i < values.size(); i++) {
            BigDecimal diff = values.get(i).subtract(sma);
            variance = variance.add(diff.multiply(diff));
        }
        
        variance = variance.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }
    
    /**
     * Update technical indicators for a trading pair
     */
    public void updateTechnicalIndicators(TradingPair pair, List<TradingPair> historicalData) {
        if (historicalData.size() < 50) {
            return;
        }
        
        // Extract prices for calculations
        List<BigDecimal> prices = new ArrayList<>();
        for (TradingPair data : historicalData) {
            if (data.getPrice() != null) {
                prices.add(data.getPrice());
            }
        }
        
        // Get indicator settings from config
        com.bitunix.scalper.model.TradingConfig config = configService.getActiveConfig();
        int rsiPeriod = config.getRsiPeriod() != null ? config.getRsiPeriod() : 14;
        int bollingerPeriod = config.getBollingerPeriod() != null ? config.getBollingerPeriod() : 20;
        double bollingerStdDev = config.getBollingerStdDev() != null ? config.getBollingerStdDev() : 2.0;
        int emaFastPeriod = config.getEmaFastPeriod() != null ? config.getEmaFastPeriod() : 12;
        int emaSlowPeriod = config.getEmaSlowPeriod() != null ? config.getEmaSlowPeriod() : 26;
        int supportResistancePeriod = config.getSupportResistancePeriod() != null ? 
                                      config.getSupportResistancePeriod() : 50;
        
        int maxPeriod = Math.max(Math.max(rsiPeriod, bollingerPeriod), 
                                Math.max(emaSlowPeriod, supportResistancePeriod));
        
        if (prices.size() < maxPeriod) {
            return;
        }
        
        // Calculate RSI
        BigDecimal rsi = calculateRSI(prices, rsiPeriod);
        if (rsi != null) {
            pair.setRsi(rsi);
        }
        
        // Calculate Bollinger Bands
        BigDecimal[] bollingerBands = calculateBollingerBands(prices, bollingerPeriod, bollingerStdDev);
        if (bollingerBands != null) {
            pair.setBollingerUpper(bollingerBands[0]);
            pair.setBollingerLower(bollingerBands[2]);
        }
        
        // Calculate EMAs
        BigDecimal emaFast = calculateEMA(prices, emaFastPeriod);
        BigDecimal emaSlow = calculateEMA(prices, emaSlowPeriod);
        if (emaFast != null) {
            pair.setEma12(emaFast);
        }
        if (emaSlow != null) {
            pair.setEma26(emaSlow);
        }
        
        // Calculate Support and Resistance
        BigDecimal[] supportResistance = calculateSupportResistance(prices, supportResistancePeriod);
        if (supportResistance != null) {
            pair.setSupportLevel(supportResistance[0]);
            pair.setResistanceLevel(supportResistance[1]);
        }
    }
}
