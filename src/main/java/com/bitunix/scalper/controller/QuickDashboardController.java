package com.bitunix.scalper.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class QuickDashboardController {
    
    @GetMapping("/quick")
    public String quickDashboard(Model model) {
        // Add demo data directly without API calls
        model.addAttribute("selectedPair", createDemoPair());
        model.addAttribute("totalProfit", BigDecimal.valueOf(1250.50));
        model.addAttribute("successRate", BigDecimal.valueOf(68.5));
        model.addAttribute("totalTrades", 45);
        model.addAttribute("successfulTrades", 31);
        model.addAttribute("activePairs", createDemoPairs());
        model.addAttribute("tradingEnabled", false);
        model.addAttribute("activeTradesCount", 0);
        
        // Rate Limiter information
        model.addAttribute("bitunixRequests", 0);
        model.addAttribute("binanceRequests", 0);
        model.addAttribute("coingeckoRequests", 0);
        model.addAttribute("tradingCycleRequests", 0);
        
        return "dashboard";
    }
    
    private Object createDemoPair() {
        // Create a simple demo pair object
        return new Object() {
            public String getSymbol() { return "BTCUSDT"; }
            public BigDecimal getPrice() { return BigDecimal.valueOf(45000.0); }
            public BigDecimal getVolume24h() { return BigDecimal.valueOf(1000000.0); }
            public BigDecimal getPriceChange24h() { return BigDecimal.valueOf(2.5); }
            public BigDecimal getRsi() { return BigDecimal.valueOf(45.0); }
            public BigDecimal getBollingerUpper() { return BigDecimal.valueOf(46000.0); }
            public BigDecimal getBollingerLower() { return BigDecimal.valueOf(44000.0); }
            public BigDecimal getEma12() { return BigDecimal.valueOf(44800.0); }
            public BigDecimal getEma26() { return BigDecimal.valueOf(44600.0); }
        };
    }
    
    private List<Object> createDemoPairs() {
        List<Object> pairs = new ArrayList<>();
        
        String[] symbols = {"BTCUSDT", "ETHUSDT", "ADAUSDT", "DOTUSDT", "LINKUSDT"};
        double[] prices = {45000.0, 3000.0, 0.5, 25.0, 15.0};
        double[] volumes = {1000000.0, 800000.0, 500000.0, 300000.0, 200000.0};
        double[] changes = {2.5, -1.2, 3.8, -0.5, 1.5};
        
        for (int i = 0; i < symbols.length; i++) {
            final String symbol = symbols[i];
            final double price = prices[i];
            final double volume = volumes[i];
            final double change = changes[i];
            
            pairs.add(new Object() {
                public String getSymbol() { return symbol; }
                public BigDecimal getPrice() { return BigDecimal.valueOf(price); }
                public BigDecimal getVolume24h() { return BigDecimal.valueOf(volume); }
                public BigDecimal getPriceChange24h() { return BigDecimal.valueOf(change); }
                public BigDecimal getRsi() { return BigDecimal.valueOf(30 + Math.random() * 40); }
                public Boolean getIsActive() { return true; }
            });
        }
        
        return pairs;
    }
}
