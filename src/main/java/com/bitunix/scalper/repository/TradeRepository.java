package com.bitunix.scalper.repository;

import com.bitunix.scalper.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    List<Trade> findByStatus(Trade.TradeStatus status);
    
    List<Trade> findBySymbol(String symbol);
    
    List<Trade> findByStrategy(String strategy);
    
    List<Trade> findByEntryTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT t FROM Trade t WHERE t.status = 'CLOSED' AND t.profit > 0")
    List<Trade> findSuccessfulTrades();
    
    @Query("SELECT t FROM Trade t WHERE t.status = 'CLOSED'")
    List<Trade> findClosedTrades();
    
    @Query("SELECT SUM(t.profit) FROM Trade t WHERE t.status = 'CLOSED'")
    BigDecimal calculateTotalProfit();
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.status = 'CLOSED'")
    Long countClosedTrades();
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.status = 'CLOSED' AND t.profit > 0")
    Long countSuccessfulTrades();
}
