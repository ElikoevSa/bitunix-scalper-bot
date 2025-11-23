package com.bitunix.scalper.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimiterService {
    
    private final ConcurrentHashMap<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTimes = new ConcurrentHashMap<>();
    
    // Максимум 1 запрос в 90 секунд
    private static final int MAX_REQUESTS_PER_WINDOW = 1;
    private static final long TIME_WINDOW_MS = 90000; // 90 секунд
    
    /**
     * Проверяет, можно ли выполнить запрос
     */
    public boolean canMakeRequest(String apiName) {
        long currentTime = System.currentTimeMillis();
        String key = apiName;
        
        // Получаем или создаем счетчик для данного API
        AtomicLong counter = requestCounters.computeIfAbsent(key, k -> new AtomicLong(0));
        Long lastResetTime = lastResetTimes.get(key);
        
        // Если прошло 90 секунд, сбрасываем счетчик
        if (lastResetTime == null || (currentTime - lastResetTime) >= TIME_WINDOW_MS) {
            counter.set(0);
            lastResetTimes.put(key, currentTime);
        }
        
        // Проверяем, не превышен ли лимит
        long currentCount = counter.incrementAndGet();
        return currentCount <= MAX_REQUESTS_PER_WINDOW;
    }
    
    /**
     * Ждет, если лимит превышен
     */
    public void waitIfNeeded(String apiName) {
        while (!canMakeRequest(apiName)) {
            try {
                long timeUntilReset = getTimeUntilReset(apiName);
                if (timeUntilReset > 0) {
                    // Ждем до сброса счетчика или максимум 1 секунду перед повторной проверкой
                    Thread.sleep(Math.min(timeUntilReset, 1000));
                } else {
                    Thread.sleep(1000); // Ждем 1 секунду перед повторной проверкой
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Получает текущий счетчик запросов
     */
    public long getCurrentRequestCount(String apiName) {
        AtomicLong counter = requestCounters.get(apiName);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Получает оставшееся время до сброса счетчика
     */
    public long getTimeUntilReset(String apiName) {
        Long lastResetTime = lastResetTimes.get(apiName);
        if (lastResetTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastResetTime;
        long timeUntilReset = TIME_WINDOW_MS - timePassed;
        
        return Math.max(0, timeUntilReset);
    }
    
    /**
     * Сбрасывает счетчик для конкретного API
     */
    public void resetCounter(String apiName) {
        requestCounters.remove(apiName);
        lastResetTimes.remove(apiName);
    }
    
    /**
     * Сбрасывает все счетчики
     */
    public void resetAllCounters() {
        requestCounters.clear();
        lastResetTimes.clear();
    }
}
