package com.bitunix.scalper.strategy;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RSIScalpingStrategy implements TradingStrategyInterface {
    
    private static final String STRATEGY_NAME = "RSI Scalping";
    private static final double RSI_OVERSOLD = 30.0;
    private static final double RSI_OVERBOUGHT = 70.0;
    private static final double RSI_EXIT_OVERSOLD = 50.0;
    private static final double RSI_EXIT_OVERBOUGHT = 50.0;
    private static final int PRIORITY = 5;
    
    @Override
    public String getName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean shouldEnter(TradingPair pair, List<TradingPair> historicalData) {
        if (pair.getRsi() == null || historicalData.size() < 14) {
            return false;
        }
        
        double rsi = pair.getRsi().doubleValue();
        
        // Enter long position when RSI is oversold
        if (rsi < RSI_OVERSOLD) {
            return true;
        }
        
        // Enter short position when RSI is overbought
        if (rsi > RSI_OVERBOUGHT) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldExit(Trade trade, TradingPair currentPair, List<TradingPair> historicalData) {
        if (currentPair.getRsi() == null) {
            return false;
        }
        
        double rsi = currentPair.getRsi().doubleValue();
        
        // Exit long position when RSI reaches neutral zone
        if (trade.getType() == Trade.TradeType.BUY && rsi > RSI_EXIT_OVERSOLD) {
            return true;
        }
        
        // Exit short position when RSI reaches neutral zone
        if (trade.getType() == Trade.TradeType.SELL && rsi < RSI_EXIT_OVERBOUGHT) {
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
        // Risk 2% of available balance per trade
        return availableBalance * 0.02;
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
