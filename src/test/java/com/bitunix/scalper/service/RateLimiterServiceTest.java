package com.bitunix.scalper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {
    
    private RateLimiterService rateLimiterService;
    
    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }
    
    @Test
    void testCanMakeRequestWithinLimit() {
        String apiName = "test-api";
        
        // Первые 7 запросов должны быть разрешены
        for (int i = 0; i < 7; i++) {
            assertTrue(rateLimiterService.canMakeRequest(apiName), 
                      "Request " + (i + 1) + " should be allowed");
        }
        
        // 8-й запрос должен быть заблокирован
        assertFalse(rateLimiterService.canMakeRequest(apiName), 
                   "Request 8 should be blocked");
    }
    
    @Test
    void testRequestCountTracking() {
        String apiName = "test-api";
        
        // Проверяем, что счетчик увеличивается
        assertEquals(1, rateLimiterService.getCurrentRequestCount(apiName));
        rateLimiterService.canMakeRequest(apiName);
        assertEquals(2, rateLimiterService.getCurrentRequestCount(apiName));
    }
    
    @Test
    void testResetCounter() {
        String apiName = "test-api";
        
        // Делаем несколько запросов
        for (int i = 0; i < 5; i++) {
            rateLimiterService.canMakeRequest(apiName);
        }
        
        // Проверяем, что счетчик увеличился
        assertTrue(rateLimiterService.getCurrentRequestCount(apiName) > 0);
        
        // Сбрасываем счетчик
        rateLimiterService.resetCounter(apiName);
        
        // Проверяем, что счетчик сброшен
        assertEquals(0, rateLimiterService.getCurrentRequestCount(apiName));
    }
    
    @Test
    void testMultipleApisIndependent() {
        String api1 = "api1";
        String api2 = "api2";
        
        // Делаем 7 запросов к первому API
        for (int i = 0; i < 7; i++) {
            assertTrue(rateLimiterService.canMakeRequest(api1));
        }
        
        // Проверяем, что первый API заблокирован
        assertFalse(rateLimiterService.canMakeRequest(api1));
        
        // Проверяем, что второй API еще работает
        assertTrue(rateLimiterService.canMakeRequest(api2));
    }
}
