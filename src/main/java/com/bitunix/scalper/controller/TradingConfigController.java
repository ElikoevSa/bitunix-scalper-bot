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

