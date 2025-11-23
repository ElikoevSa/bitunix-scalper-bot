package com.bitunix.scalper.config;

import com.bitunix.scalper.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimiterService rateLimiterService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String clientIP = getClientIP(request);
        
        // Создаем уникальный ключ для комбинации IP + endpoint
        String rateLimitKey = clientIP + ":" + requestURI;
        
        // Проверяем rate limit для данного клиента и endpoint
        if (!rateLimiterService.canMakeRequest(rateLimitKey)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            long timeUntilReset = rateLimiterService.getTimeUntilReset(rateLimitKey);
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Maximum 1 request per 90 seconds. Wait " + 
                                      (timeUntilReset / 1000) + " seconds.\"}");
            return false;
        }
        
        return true;
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
