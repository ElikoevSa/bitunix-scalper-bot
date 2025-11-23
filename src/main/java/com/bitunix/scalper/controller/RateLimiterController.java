package com.bitunix.scalper.controller;

import com.bitunix.scalper.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-limiter")
public class RateLimiterController {
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    /**
     * Получить статус Rate Limiter для всех API
     */
    @GetMapping("/status")
    public Map<String, Object> getRateLimiterStatus() {
        Map<String, Object> status = new HashMap<>();
        
        String[] apis = {"bitunix", "binance", "coingecko", "trading_cycle", "dashboard", "trading_control"};
        
        for (String api : apis) {
            Map<String, Object> apiStatus = new HashMap<>();
            apiStatus.put("currentRequests", rateLimiterService.getCurrentRequestCount(api));
            apiStatus.put("timeUntilReset", rateLimiterService.getTimeUntilReset(api));
            apiStatus.put("canMakeRequest", rateLimiterService.canMakeRequest(api));
            
            status.put(api, apiStatus);
        }
        
        return status;
    }
    
    /**
     * Сбросить счетчики для конкретного API
     */
    @GetMapping("/reset/{apiName}")
    public Map<String, String> resetCounter(@PathVariable String apiName) {
        rateLimiterService.resetCounter(apiName);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Counter reset for API: " + apiName);
        response.put("status", "success");
        
        return response;
    }
    
    /**
     * Сбросить все счетчики
     */
    @GetMapping("/reset-all")
    public Map<String, String> resetAllCounters() {
        rateLimiterService.resetAllCounters();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All counters reset");
        response.put("status", "success");
        
        return response;
    }
    
    /**
     * Получить информацию о rate limiting для конкретного API
     */
    @GetMapping("/info/{apiName}")
    public Map<String, Object> getApiInfo(@PathVariable String apiName) {
        Map<String, Object> info = new HashMap<>();
        info.put("apiName", apiName);
        info.put("currentRequests", rateLimiterService.getCurrentRequestCount(apiName));
        info.put("timeUntilReset", rateLimiterService.getTimeUntilReset(apiName));
        info.put("canMakeRequest", rateLimiterService.canMakeRequest(apiName));
        info.put("maxRequestsPerSecond", 7);
        
        return info;
    }
    
    /**
     * Проверить, можно ли сделать запрос к конкретному API
     */
    @GetMapping("/check/{apiName}")
    public Map<String, Object> checkRequest(@PathVariable String apiName) {
        Map<String, Object> result = new HashMap<>();
        boolean canMakeRequest = rateLimiterService.canMakeRequest(apiName);
        
        result.put("apiName", apiName);
        result.put("canMakeRequest", canMakeRequest);
        result.put("currentRequests", rateLimiterService.getCurrentRequestCount(apiName));
        result.put("timeUntilReset", rateLimiterService.getTimeUntilReset(apiName));
        
        if (!canMakeRequest) {
            result.put("message", "Rate limit exceeded. Please wait " + 
                         rateLimiterService.getTimeUntilReset(apiName) + "ms");
        } else {
            result.put("message", "Request allowed");
        }
        
        return result;
    }
}

