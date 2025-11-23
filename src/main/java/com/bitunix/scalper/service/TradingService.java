package com.bitunix.scalper.service;

import com.bitunix.scalper.model.Trade;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradingService {
    
    @Autowired
    private List<TradingStrategyInterface> strategies;
    
    @Value("${bitunix.trading.maker-fee}")
    private BigDecimal makerFee;
    
    @Value("${bitunix.trading.taker-fee}")
    private BigDecimal takerFee;
    
    @Autowired
    private TradingConfigService configService;
    
    /**
     * Execute a trade based on strategy signals
     */
    public Trade executeTrade(TradingPair pair, TradingStrategyInterface strategy, 
                            List<TradingPair> historicalData, double availableBalance) {
        
        if (!strategy.isActive()) {
            return null;
        }
        
        // Check if strategy signals entry
        if (!strategy.shouldEnter(pair, historicalData)) {
            return null;
        }
        
        // Calculate trade parameters
        double entryPrice = strategy.calculateEntryPrice(pair);
        
        // Calculate position size based on percentage of balance
        com.bitunix.scalper.model.TradingConfig config = configService.getActiveConfig();
        double positionSizePercent = config.getPositionSizePercent() != null ? 
            config.getPositionSizePercent() : 5.0; // Default 5%
        
        // Calculate position size as percentage of balance
        double positionSize = (availableBalance * positionSizePercent) / 100.0;
        
        // Also consider strategy's own position size calculation
        double strategyPositionSize = strategy.calculatePositionSize(pair, availableBalance);
        
        // Use the smaller of the two (more conservative)
        positionSize = Math.min(positionSize, strategyPositionSize);
        
        // Determine trade type based on strategy
        Trade.TradeType tradeType = determineTradeType(pair, strategy);
        
        // Create trade
        Trade trade = new Trade();
        trade.setSymbol(pair.getSymbol());
        trade.setType(tradeType);
        trade.setStatus(Trade.TradeStatus.OPEN);
        trade.setEntryPrice(BigDecimal.valueOf(entryPrice));
        trade.setQuantity(BigDecimal.valueOf(positionSize / entryPrice));
        trade.setStrategy(strategy.getName());
        trade.setEntryTime(LocalDateTime.now());
        
        // Calculate fees
        BigDecimal tradeValue = BigDecimal.valueOf(positionSize);
        BigDecimal makerFeeAmount = tradeValue.multiply(makerFee);
        BigDecimal takerFeeAmount = tradeValue.multiply(takerFee);
        
        trade.setMakerFee(makerFeeAmount);
        trade.setTakerFee(takerFeeAmount);
        trade.setTotalFees(makerFeeAmount.add(takerFeeAmount));
        
        return trade;
    }
    
    /**
     * Close a trade
     */
    public Trade closeTrade(Trade trade, TradingPair currentPair, TradingStrategyInterface strategy) {
        if (trade.getStatus() != Trade.TradeStatus.OPEN) {
            return trade;
        }
        
        // Check if strategy signals exit
        if (!strategy.shouldExit(trade, currentPair, null)) {
            return trade;
        }
        
        // Calculate exit price
        double exitPrice = strategy.calculateExitPrice(trade, currentPair);
        trade.setExitPrice(BigDecimal.valueOf(exitPrice));
        trade.setExitTime(LocalDateTime.now());
        trade.setStatus(Trade.TradeStatus.CLOSED);
        
        // Calculate profit/loss
        BigDecimal entryValue = trade.getEntryPrice().multiply(trade.getQuantity());
        BigDecimal exitValue = trade.getExitPrice().multiply(trade.getQuantity());
        BigDecimal grossProfit = exitValue.subtract(entryValue);
        
        // Apply fees
        BigDecimal netProfit = grossProfit.subtract(trade.getTotalFees());
        trade.setProfit(netProfit);
        
        // Calculate profit percentage
        BigDecimal profitPercentage = netProfit.divide(entryValue, 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));
        trade.setProfitPercentage(profitPercentage);
        
        return trade;
    }
    
    /**
     * Calculate total profit/loss for all trades
     */
    public BigDecimal calculateTotalProfit(List<Trade> trades) {
        return trades.stream()
                .filter(trade -> trade.getStatus() == Trade.TradeStatus.CLOSED)
                .map(Trade::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate success rate
     */
    public BigDecimal calculateSuccessRate(List<Trade> trades) {
        long totalTrades = trades.stream()
                .filter(trade -> trade.getStatus() == Trade.TradeStatus.CLOSED)
                .count();
        
        if (totalTrades == 0) {
            return BigDecimal.ZERO;
        }
        
        long successfulTrades = trades.stream()
                .filter(trade -> trade.getStatus() == Trade.TradeStatus.CLOSED)
                .filter(trade -> trade.getProfit().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        return BigDecimal.valueOf(successfulTrades)
                .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Determine trade type based on strategy and market conditions
     */
    private Trade.TradeType determineTradeType(TradingPair pair, TradingStrategyInterface strategy) {
        // This is a simplified logic - in real implementation, 
        // you would analyze market conditions more thoroughly
        
        // For RSI strategy
        if (strategy.getName().equals("RSI Scalping")) {
            if (pair.getRsi() != null && pair.getRsi().doubleValue() < 30) {
                return Trade.TradeType.BUY;
            } else if (pair.getRsi() != null && pair.getRsi().doubleValue() > 70) {
                return Trade.TradeType.SELL;
            }
        }
        
        // For Bollinger Bands strategy
        if (strategy.getName().equals("Bollinger Bounce")) {
            if (pair.getBollingerLower() != null && 
                pair.getPrice().compareTo(pair.getBollingerLower()) <= 0) {
                return Trade.TradeType.BUY;
            } else if (pair.getBollingerUpper() != null && 
                      pair.getPrice().compareTo(pair.getBollingerUpper()) >= 0) {
                return Trade.TradeType.SELL;
            }
        }
        
        // Default to BUY for other strategies
        return Trade.TradeType.BUY;
    }
}
