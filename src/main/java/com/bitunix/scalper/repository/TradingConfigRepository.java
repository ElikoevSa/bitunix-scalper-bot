package com.bitunix.scalper.repository;

import com.bitunix.scalper.model.TradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingConfigRepository extends JpaRepository<TradingConfig, Long> {
    
    Optional<TradingConfig> findByName(String name);
    
    Optional<TradingConfig> findByIsActiveTrue();
}

