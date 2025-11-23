package com.bitunix.scalper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;

/**
 * Trading configuration model
 * Stores selected strategies and trading pairs
 */
@Entity
@Table(name = "trading_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name = "default";
    
    // Selected strategy names (comma-separated or JSON array)
    @Column(columnDefinition = "TEXT")
    private String selectedStrategies; // JSON array of strategy names
    
    // Selected trading pairs (comma-separated or JSON array)
    @Column(columnDefinition = "TEXT")
    private String selectedPairs; // JSON array of symbols
    
    // Risk management - percentage based
    @Column(precision = 10, scale = 4)
    private Double positionSizePercent; // % of balance per trade
    
    @Column(precision = 10, scale = 4)
    private Double stopLossPercent; // % stop loss
    
    @Column(precision = 10, scale = 4)
    private Double takeProfitPercent; // % take profit
    
    @Column(precision = 10, scale = 4)
    private Double maxDailyLossPercent; // % max daily loss
    
    // Strategy selection mode
    @Column
    private Boolean autoSelectBestStrategy = true; // Auto-select best strategy from selected list
    
    // Minimum score for strategy selection
    @Column(precision = 10, scale = 4)
    private Double minStrategyScore = 0.5; // Minimum score (0-1) for strategy to be selected
    
    // Indicator settings
    @Column
    private Integer rsiPeriod = 14; // RSI calculation period
    
    @Column
    private Integer bollingerPeriod = 20; // Bollinger Bands period
    @Column(precision = 10, scale = 2)
    private Double bollingerStdDev = 2.0; // Bollinger Bands standard deviation multiplier
    
    @Column
    private Integer emaFastPeriod = 12; // Fast EMA period
    @Column
    private Integer emaSlowPeriod = 26; // Slow EMA period
    
    @Column
    private Integer supportResistancePeriod = 50; // Support/Resistance lookback period
    
    // API Configuration
    @Column(columnDefinition = "TEXT")
    private String apiBaseUrl; // API base URL
    
    @Column(columnDefinition = "TEXT")
    private String apiKey; // API key
    
    @Column(columnDefinition = "TEXT")
    private String apiSecretKey; // API secret key
    
    @Column
    private Boolean isActive = true;
}

