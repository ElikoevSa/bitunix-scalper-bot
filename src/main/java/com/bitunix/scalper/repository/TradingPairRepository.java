package com.bitunix.scalper.repository;

import com.bitunix.scalper.model.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {
    
    TradingPair findBySymbol(String symbol);
    
    List<TradingPair> findByIsActiveTrue();
    
    List<TradingPair> findByIsActiveFalse();
    
    @Query("SELECT t FROM TradingPair t WHERE t.isActive = true ORDER BY t.volume24h DESC")
    List<TradingPair> findActivePairsOrderByVolume();
    
    @Query("SELECT t FROM TradingPair t WHERE t.isActive = true AND t.volume24h > :minVolume ORDER BY t.volume24h DESC")
    List<TradingPair> findActivePairsWithMinVolume(Double minVolume);
}
