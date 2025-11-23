package com.bitunix.scalper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_pairs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String symbol;
    
    private String baseAsset;
    private String quoteAsset;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal volume24h;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal priceChange24h;
    
    private Boolean isActive;
    private LocalDateTime lastUpdated;
    
    // Technical indicators
    @Column(precision = 10, scale = 4)
    private BigDecimal rsi;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal bollingerUpper;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal bollingerLower;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal ema12;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal ema26;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal supportLevel;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal resistanceLevel;
    
    private Integer volumeSpike;
}
