package com.bitunix.scalper.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimiterInterceptor rateLimiterInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate limiting отключен для веб-эндпоинтов
        // Применяется только к API бирж через сервисы (BitunixApiService, BybitDemoTradingService)
        // Исключаем все веб-эндпоинты из rate limiting
        registry.addInterceptor(rateLimiterInterceptor)
                .excludePathPatterns(
                    "/**"              // Исключаем все веб-эндпоинты
                );
    }
}
