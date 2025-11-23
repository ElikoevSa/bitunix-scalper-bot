package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for evaluating and ranking trading strategies
 */
@Service
public class StrategyEvaluationService {
    
    /**
     * Evaluate strategy for a given trading pair
     * Returns score from 0.0 to 1.0
     */
    public double evaluateStrategy(TradingPair pair, TradingStrategyInterface strategy, 
                                  List<TradingPair> historicalData) {
        double score = 0.0;
        
        // Factor 1: Strategy signal strength (0-0.4)
        if (strategy.shouldEnter(pair, historicalData)) {
            double signalStrength = calculateSignalStrength(pair, strategy);
            score += signalStrength * 0.4;
        }
        
        // Factor 2: Technical indicators alignment (0-0.3)
        double indicatorScore = evaluateTechnicalIndicators(pair, strategy);
        score += indicatorScore * 0.3;
        
        // Factor 3: Volume and liquidity (0-0.2)
        double volumeScore = evaluateVolume(pair);
        score += volumeScore * 0.2;
        
        // Factor 4: Strategy priority (0-0.1)
        double priorityScore = strategy.getPriority() / 10.0; // Normalize to 0-1
        score += Math.min(priorityScore, 1.0) * 0.1;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Calculate signal strength for strategy
     */
    private double calculateSignalStrength(TradingPair pair, TradingStrategyInterface strategy) {
        String strategyName = strategy.getName();
        
        // RSI Scalping
        if (strategyName.contains("RSI")) {
            if (pair.getRsi() != null) {
                double rsi = pair.getRsi().doubleValue();
                if (rsi < 30) {
                    return 1.0 - (rsi / 30.0); // Stronger signal when RSI is lower
                } else if (rsi > 70) {
                    return 1.0 - ((100 - rsi) / 30.0); // Stronger signal when RSI is higher
                }
            }
        }
        
        // Bollinger Bounce
        if (strategyName.contains("Bollinger")) {
            if (pair.getBollingerLower() != null && pair.getBollingerUpper() != null) {
                double price = pair.getPrice().doubleValue();
                double lower = pair.getBollingerLower().doubleValue();
                double upper = pair.getBollingerUpper().doubleValue();
                double range = upper - lower;
                
                if (range > 0) {
                    double distanceFromLower = (price - lower) / range;
                    double distanceFromUpper = (upper - price) / range;
                    return Math.max(distanceFromLower, distanceFromUpper);
                }
            }
        }
        
        // EMA Crossover
        if (strategyName.contains("EMA")) {
            if (pair.getEma12() != null && pair.getEma26() != null) {
                double ema12 = pair.getEma12().doubleValue();
                double ema26 = pair.getEma26().doubleValue();
                
                if (ema26 > 0) {
                    double crossoverStrength = Math.abs(ema12 - ema26) / ema26;
                    return Math.min(crossoverStrength * 10, 1.0);
                }
            }
        }
        
        // Default: moderate signal
        return 0.5;
    }
    
    /**
     * Evaluate technical indicators alignment
     */
    private double evaluateTechnicalIndicators(TradingPair pair, TradingStrategyInterface strategy) {
        double score = 0.5; // Base score
        
        // RSI alignment
        if (pair.getRsi() != null) {
            double rsi = pair.getRsi().doubleValue();
            if (rsi > 20 && rsi < 80) {
                score += 0.2; // Good RSI range
            }
        }
        
        // Volume alignment
        if (pair.getVolume24h() != null && pair.getVolume24h().doubleValue() > 10000) {
            score += 0.2; // Good volume
        }
        
        // Price movement
        if (pair.getPriceChange24h() != null) {
            double change = Math.abs(pair.getPriceChange24h().doubleValue());
            if (change > 0.5 && change < 5.0) {
                score += 0.1; // Moderate price movement
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Evaluate volume and liquidity
     */
    private double evaluateVolume(TradingPair pair) {
        if (pair.getVolume24h() == null) {
            return 0.0;
        }
        
        double volume = pair.getVolume24h().doubleValue();
        
        // Normalize volume score (0-1)
        // Consider volumes > 100k as good
        if (volume > 1000000) {
            return 1.0;
        } else if (volume > 100000) {
            return 0.8;
        } else if (volume > 10000) {
            return 0.5;
        } else {
            return 0.2;
        }
    }
    
    /**
     * Find best strategy for a trading pair from a list of strategies
     */
    public TradingStrategyInterface findBestStrategy(TradingPair pair, 
                                                     List<TradingStrategyInterface> strategies,
                                                     List<TradingPair> historicalData,
                                                     double minScore) {
        List<StrategyScore> scoredStrategies = new ArrayList<>();
        
        for (TradingStrategyInterface strategy : strategies) {
            if (!strategy.isActive()) {
                continue;
            }
            
            double score = evaluateStrategy(pair, strategy, historicalData);
            if (score >= minScore) {
                scoredStrategies.add(new StrategyScore(strategy, score));
            }
        }
        
        if (scoredStrategies.isEmpty()) {
            return null;
        }
        
        // Sort by score descending, then by priority
        scoredStrategies.sort(Comparator
                .comparing(StrategyScore::getScore).reversed()
                .thenComparing(s -> s.getStrategy().getPriority()).reversed());
        
        return scoredStrategies.get(0).getStrategy();
    }
    
    /**
     * Inner class for strategy scoring
     */
    private static class StrategyScore {
        private final TradingStrategyInterface strategy;
        private final double score;
        
        public StrategyScore(TradingStrategyInterface strategy, double score) {
            this.strategy = strategy;
            this.score = score;
        }
        
        public TradingStrategyInterface getStrategy() {
            return strategy;
        }
        
        public double getScore() {
            return score;
        }
    }
}

