package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingConfig;
import com.bitunix.scalper.repository.TradingConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing trading configuration
 */
@Service
public class TradingConfigService {
    
    @Autowired
    private TradingConfigRepository configRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get active trading configuration
     */
    public TradingConfig getActiveConfig() {
        return configRepository.findByIsActiveTrue()
                .orElseGet(this::createDefaultConfig);
    }
    
    /**
     * Get configuration by name
     */
    public TradingConfig getConfig(String name) {
        return configRepository.findByName(name)
                .orElseGet(this::createDefaultConfig);
    }
    
    /**
     * Save or update configuration
     */
    @Transactional
    public TradingConfig saveConfig(TradingConfig config) {
        // If this config is set as active, deactivate others
        if (config.getIsActive() != null && config.getIsActive()) {
            List<TradingConfig> activeConfigs = configRepository.findAll().stream()
                    .filter(c -> c.getIsActive() != null && c.getIsActive() && !c.getId().equals(config.getId()))
                    .collect(java.util.stream.Collectors.toList());
            for (TradingConfig activeConfig : activeConfigs) {
                activeConfig.setIsActive(false);
                configRepository.save(activeConfig);
            }
        }
        
        return configRepository.save(config);
    }
    
    /**
     * Get selected strategy names
     */
    public List<String> getSelectedStrategies() {
        TradingConfig config = getActiveConfig();
        return parseStringList(config.getSelectedStrategies());
    }
    
    /**
     * Set selected strategy names
     */
    @Transactional
    public void setSelectedStrategies(List<String> strategyNames) {
        TradingConfig config = getActiveConfig();
        config.setSelectedStrategies(stringifyList(strategyNames));
        saveConfig(config);
    }
    
    /**
     * Get selected trading pairs
     */
    public List<String> getSelectedPairs() {
        TradingConfig config = getActiveConfig();
        return parseStringList(config.getSelectedPairs());
    }
    
    /**
     * Set selected trading pairs
     */
    @Transactional
    public void setSelectedPairs(List<String> pairs) {
        TradingConfig config = getActiveConfig();
        config.setSelectedPairs(stringifyList(pairs));
        saveConfig(config);
    }
    
    /**
     * Check if strategy is selected
     */
    public boolean isStrategySelected(String strategyName) {
        List<String> selected = getSelectedStrategies();
        return selected.isEmpty() || selected.contains(strategyName);
    }
    
    /**
     * Check if pair is selected
     */
    public boolean isPairSelected(String symbol) {
        List<String> selected = getSelectedPairs();
        return selected.isEmpty() || selected.contains(symbol);
    }
    
    /**
     * Create default configuration
     */
    private TradingConfig createDefaultConfig() {
        TradingConfig config = new TradingConfig();
        config.setName("default");
        config.setSelectedStrategies("[]"); // Empty = all strategies
        config.setSelectedPairs("[]"); // Empty = all pairs
        config.setPositionSizePercent(5.0); // 5% of balance per trade
        config.setStopLossPercent(0.5); // 0.5% stop loss
        config.setTakeProfitPercent(0.3); // 0.3% take profit
        config.setMaxDailyLossPercent(10.0); // 10% max daily loss
        config.setAutoSelectBestStrategy(true);
        config.setMinStrategyScore(0.5);
        config.setIsActive(true);
        
        return configRepository.save(config);
    }
    
    /**
     * Parse JSON string list
     */
    private List<String> parseStringList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty() || jsonString.equals("[]")) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Fallback: try comma-separated
            List<String> result = new ArrayList<>();
            String[] parts = jsonString.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
    }
    
    /**
     * Convert list to JSON string
     */
    private String stringifyList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            // Fallback: comma-separated
            return String.join(",", list);
        }
    }
}

