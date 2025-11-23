package com.bitunix.scalper.controller;

import com.bitunix.scalper.model.TradingConfig;
import com.bitunix.scalper.service.TradingConfigService;
import com.bitunix.scalper.strategy.TradingStrategyInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for managing trading configuration
 */
@Controller
@RequestMapping("/config")
public class TradingConfigController {
    
    @Autowired
    private TradingConfigService configService;
    
    @Autowired
    private List<TradingStrategyInterface> strategies;
    
    /**
     * Configuration page
     */
    @GetMapping
    public String configPage(Model model) {
        TradingConfig config = configService.getActiveConfig();
        List<String> selectedStrategies = configService.getSelectedStrategies();
        List<String> selectedPairs = configService.getSelectedPairs();
        
        // Get all available strategy names
        List<String> allStrategyNames = strategies.stream()
                .map(TradingStrategyInterface::getName)
                .collect(Collectors.toList());
        
        model.addAttribute("config", config);
        model.addAttribute("selectedStrategies", selectedStrategies);
        model.addAttribute("selectedPairs", selectedPairs);
        model.addAttribute("allStrategies", allStrategyNames);
        
        return "trading-config";
    }
    
    /**
     * Update selected strategies
     */
    @PostMapping("/strategies")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStrategies(@RequestParam("strategies") List<String> strategyNames) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            configService.setSelectedStrategies(strategyNames);
            response.put("success", true);
            response.put("message", "Strategies updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update selected pairs
     */
    @PostMapping("/pairs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePairs(@RequestParam("pairs") List<String> pairs) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            configService.setSelectedPairs(pairs);
            response.put("success", true);
            response.put("message", "Trading pairs updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update risk management settings
     */
    @PostMapping("/risk")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRiskManagement(
            @RequestParam(required = false) Double positionSizePercent,
            @RequestParam(required = false) Double stopLossPercent,
            @RequestParam(required = false) Double takeProfitPercent,
            @RequestParam(required = false) Double maxDailyLossPercent) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TradingConfig config = configService.getActiveConfig();
            
            if (positionSizePercent != null) {
                config.setPositionSizePercent(positionSizePercent);
            }
            if (stopLossPercent != null) {
                config.setStopLossPercent(stopLossPercent);
            }
            if (takeProfitPercent != null) {
                config.setTakeProfitPercent(takeProfitPercent);
            }
            if (maxDailyLossPercent != null) {
                config.setMaxDailyLossPercent(maxDailyLossPercent);
            }
            
            configService.saveConfig(config);
            response.put("success", true);
            response.put("message", "Risk management settings updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update strategy selection mode
     */
    @PostMapping("/strategy-selection")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStrategySelection(
            @RequestParam Boolean autoSelectBestStrategy,
            @RequestParam(required = false) Double minStrategyScore) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TradingConfig config = configService.getActiveConfig();
            config.setAutoSelectBestStrategy(autoSelectBestStrategy);
            
            if (minStrategyScore != null) {
                config.setMinStrategyScore(minStrategyScore);
            }
            
            configService.saveConfig(config);
            response.put("success", true);
            response.put("message", "Strategy selection settings updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update API settings
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateApiSettings(
            @RequestParam(required = false) String apiBaseUrl,
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String apiSecretKey) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TradingConfig config = configService.getActiveConfig();
            
            if (apiBaseUrl != null && !apiBaseUrl.trim().isEmpty()) {
                config.setApiBaseUrl(apiBaseUrl.trim());
            }
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                config.setApiKey(apiKey.trim());
            }
            if (apiSecretKey != null && !apiSecretKey.trim().isEmpty()) {
                config.setApiSecretKey(apiSecretKey.trim());
            }
            
            configService.saveConfig(config);
            response.put("success", true);
            response.put("message", "API settings updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update indicator settings
     */
    @PostMapping("/indicators")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateIndicators(
            @RequestParam(required = false) Integer rsiPeriod,
            @RequestParam(required = false) Integer bollingerPeriod,
            @RequestParam(required = false) Double bollingerStdDev,
            @RequestParam(required = false) Integer emaFastPeriod,
            @RequestParam(required = false) Integer emaSlowPeriod,
            @RequestParam(required = false) Integer supportResistancePeriod) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TradingConfig config = configService.getActiveConfig();
            
            if (rsiPeriod != null && rsiPeriod > 0) {
                config.setRsiPeriod(rsiPeriod);
            }
            if (bollingerPeriod != null && bollingerPeriod > 0) {
                config.setBollingerPeriod(bollingerPeriod);
            }
            if (bollingerStdDev != null && bollingerStdDev > 0) {
                config.setBollingerStdDev(bollingerStdDev);
            }
            if (emaFastPeriod != null && emaFastPeriod > 0) {
                config.setEmaFastPeriod(emaFastPeriod);
            }
            if (emaSlowPeriod != null && emaSlowPeriod > 0) {
                config.setEmaSlowPeriod(emaSlowPeriod);
            }
            if (supportResistancePeriod != null && supportResistancePeriod > 0) {
                config.setSupportResistancePeriod(supportResistancePeriod);
            }
            
            configService.saveConfig(config);
            response.put("success", true);
            response.put("message", "Indicator settings updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current configuration (JSON)
     */
    @GetMapping("/current")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TradingConfig config = configService.getActiveConfig();
            response.put("success", true);
            response.put("config", config);
            response.put("selectedStrategies", configService.getSelectedStrategies());
            response.put("selectedPairs", configService.getSelectedPairs());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

