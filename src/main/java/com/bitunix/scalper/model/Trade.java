package com.bitunix.scalper.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String symbol;
    
    @Enumerated(EnumType.STRING)
    private TradeType type;
    
    @Enumerated(EnumType.STRING)
    private TradeStatus status;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal entryPrice;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal exitPrice;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal quantity;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal profit;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal profitPercentage;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal makerFee;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal takerFee;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal totalFees;
    
    private String strategy;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String notes;
    
    public enum TradeType {
        BUY, SELL
    }
    
    public enum TradeStatus {
        OPEN, CLOSED, CANCELLED
    }
}
