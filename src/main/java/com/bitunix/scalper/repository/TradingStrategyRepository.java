package com.bitunix.scalper.repository;

import com.bitunix.scalper.model.TradingStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingStrategyRepository extends JpaRepository<TradingStrategy, Long> {
    
    TradingStrategy findByName(String name);
    
    List<TradingStrategy> findByIsActiveTrue();
    
    List<TradingStrategy> findByIsActiveFalse();
    
    List<TradingStrategy> findByIsActiveTrueOrderByPriorityDesc();
}
