package com.bitunix.scalper.service;

import com.bitunix.scalper.model.TradingConfig;
import com.bitunix.scalper.model.TradingPair;
import com.bitunix.scalper.repository.TradingConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing trading configuration
 */
@Service
public class TradingConfigService {
    
    @Autowired
    private TradingConfigRepository configRepository;
    
    @Autowired(required = false)
    @Lazy
    private BitunixApiService bitunixApiService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get active trading configuration
     */
    public TradingConfig getActiveConfig() {
        TradingConfig config = configRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    try {
                        return createDefaultConfig();
                    } catch (Exception e) {
                        // If creation fails (e.g., duplicate), try to find existing
                        System.err.println("Error creating default config: " + e.getMessage());
                        return configRepository.findByName("default")
                                .orElseGet(() -> {
                                    // Last resort: return a minimal config
                                    TradingConfig minimal = new TradingConfig();
                                    minimal.setName("default");
                                    minimal.setIsActive(true);
                                    return minimal;
                                });
                    }
                });
        
        // If config exists but has no selected pairs, try to populate them
        if (config != null) {
            List<String> selectedPairs = parseStringList(config.getSelectedPairs());
            if (selectedPairs.isEmpty() && config.getName().equals("default")) {
                try {
                    List<String> defaultPairs = selectDefaultPairs();
                    if (!defaultPairs.isEmpty()) {
                        config.setSelectedPairs(stringifyList(defaultPairs));
                        config = saveConfig(config);
                        System.out.println("Auto-populated default pairs: " + defaultPairs);
                    }
                } catch (Exception e) {
                    System.err.println("Error populating default pairs: " + e.getMessage());
                }
            }
        }
        
        return config;
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
    @Transactional
    private TradingConfig createDefaultConfig() {
        // Use synchronized block to prevent race conditions
        synchronized (TradingConfigService.class) {
            // First, try to find existing "default" config
            TradingConfig existingConfig = configRepository.findByName("default").orElse(null);
            if (existingConfig != null) {
                // If exists but not active, activate it
                if (existingConfig.getIsActive() == null || !existingConfig.getIsActive()) {
                    existingConfig.setIsActive(true);
                    return configRepository.save(existingConfig);
                }
                return existingConfig;
            }
            
            // Double-check after synchronization to avoid duplicate creation
            existingConfig = configRepository.findByName("default").orElse(null);
            if (existingConfig != null) {
                if (existingConfig.getIsActive() == null || !existingConfig.getIsActive()) {
                    existingConfig.setIsActive(true);
                    return configRepository.save(existingConfig);
                }
                return existingConfig;
            }
            
            // Create new default config only if it doesn't exist
            TradingConfig config = new TradingConfig();
            config.setName("default");
            config.setSelectedStrategies("[]"); // Empty = all strategies
            
            // Auto-select popular and volatile pairs (only if API is available)
            try {
                List<String> defaultPairs = selectDefaultPairs();
                if (!defaultPairs.isEmpty()) {
                    config.setSelectedPairs(stringifyList(defaultPairs));
                } else {
                    config.setSelectedPairs("[]"); // Empty if can't select
                }
            } catch (Exception e) {
                System.err.println("Error selecting default pairs: " + e.getMessage());
                config.setSelectedPairs("[]"); // Empty on error
            }
            
            config.setPositionSizePercent(5.0); // 5% of balance per trade
            config.setStopLossPercent(0.5); // 0.5% stop loss
            config.setTakeProfitPercent(0.3); // 0.3% take profit
            config.setMaxDailyLossPercent(10.0); // 10% max daily loss
            config.setAutoSelectBestStrategy(true);
            config.setMinStrategyScore(0.5);
            config.setIsActive(true);
            
            // Set default API settings from application.yml if available
            config.setApiBaseUrl("https://api-demo.bybit.com");
            
            try {
                return configRepository.save(config);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // If save fails due to duplicate, find and return existing
                System.err.println("Duplicate key violation, finding existing config: " + e.getMessage());
                return configRepository.findByName("default")
                        .orElseThrow(() -> new RuntimeException("Failed to create or find default config", e));
            }
        }
    }
    
    /**
     * Select default pairs: 10 most popular (by volume) and 5 most volatile
     */
    @Transactional
    public List<String> selectDefaultPairs() {
        List<String> selectedPairs = new ArrayList<>();
        
        if (bitunixApiService == null) {
            // If API service not available, return empty list
            return selectedPairs;
        }
        
        try {
            // Get all trading pairs
            List<TradingPair> allPairs = bitunixApiService.getAllTradingPairs();
            
            if (allPairs == null || allPairs.isEmpty()) {
                return selectedPairs;
            }
            
            // Filter active pairs with sufficient volume
            List<TradingPair> activePairs = allPairs.stream()
                    .filter(pair -> pair.getIsActive() != null && pair.getIsActive())
                    .filter(pair -> pair.getVolume24h() != null && 
                                   pair.getVolume24h().compareTo(BigDecimal.valueOf(1000)) > 0)
                    .collect(Collectors.toList());
            
            // Get 10 most popular pairs (by volume)
            List<String> popularPairs = activePairs.stream()
                    .sorted(Comparator.comparing((TradingPair p) -> 
                        p.getVolume24h() != null ? p.getVolume24h() : BigDecimal.ZERO)
                        .reversed())
                    .limit(10)
                    .map(TradingPair::getSymbol)
                    .collect(Collectors.toList());
            
            // Get 5 most volatile pairs (by price change)
            List<String> volatilePairs = activePairs.stream()
                    .filter(pair -> pair.getPriceChange24h() != null)
                    .sorted(Comparator.comparing((TradingPair p) -> 
                        p.getPriceChange24h() != null ? 
                        p.getPriceChange24h().abs() : BigDecimal.ZERO)
                        .reversed())
                    .limit(5)
                    .map(TradingPair::getSymbol)
                    .collect(Collectors.toList());
            
            // Combine and remove duplicates
            selectedPairs.addAll(popularPairs);
            for (String pair : volatilePairs) {
                if (!selectedPairs.contains(pair)) {
                    selectedPairs.add(pair);
                }
            }
            
            // Limit to 15 total (10 popular + 5 volatile, but may overlap)
            if (selectedPairs.size() > 15) {
                selectedPairs = selectedPairs.subList(0, 15);
            }
            
        } catch (Exception e) {
            System.err.println("Error selecting default pairs: " + e.getMessage());
            // Return empty list on error
        }
        
        return selectedPairs;
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

