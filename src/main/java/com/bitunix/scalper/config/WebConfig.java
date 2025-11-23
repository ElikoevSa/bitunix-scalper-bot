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
        registry.addInterceptor(rateLimiterInterceptor)
                .addPathPatterns("/**") // Применяется ко всем эндпоинтам
                .excludePathPatterns(
                    "/static/**",      // Исключаем статические ресурсы
                    "/css/**",         // Исключаем CSS файлы
                    "/js/**",          // Исключаем JavaScript файлы
                    "/images/**",      // Исключаем изображения
                    "/favicon.ico"     // Исключаем favicon
                );
    }
}
