package com.bitunix.scalper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_strategies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    private Boolean isActive;
    private Integer priority;
    
    // Strategy parameters
    @Column(precision = 10, scale = 4)
    private BigDecimal rsiOversold;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal rsiOverbought;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal bollingerPeriod;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal bollingerStdDev;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal emaShortPeriod;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal emaLongPeriod;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal volumeThreshold;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal stopLoss;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal takeProfit;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    private Integer totalTrades;
    private Integer successfulTrades;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal successRate;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal totalProfit;
}
