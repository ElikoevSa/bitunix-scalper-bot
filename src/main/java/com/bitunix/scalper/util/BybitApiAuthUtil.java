package com.bitunix.scalper.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Utility class for Bybit API authentication
 * Implements signature generation for Bybit v5 API requests
 */
public class BybitApiAuthUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    
    /**
     * Generate signature for Bybit API request (simplified version)
     * 
     * @param apiSecret API secret key
     * @param timestamp Request timestamp
     * @param recvWindow Receive window
     * @param apiKey API key
     * @param queryString Query string
     * @param requestBody Request body
     * @return Generated signature
     */
    public static String generateSignature(String apiSecret, long timestamp, 
                                          long recvWindow, String apiKey,
                                          String queryString, String requestBody) {
        try {
            // Build parameter string: timestamp + apiKey + recvWindow + queryString + requestBody
            StringBuilder paramStr = new StringBuilder();
            paramStr.append(timestamp);
            paramStr.append(apiKey);
            paramStr.append(recvWindow);
            
            if (queryString != null && !queryString.isEmpty()) {
                paramStr.append(queryString);
            }
            
            if (requestBody != null && !requestBody.isEmpty()) {
                paramStr.append(requestBody);
            }
            
            // Create HMAC SHA256 signature
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(paramStr.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }
    
    /**
     * Build query string from parameters map (sorted)
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        // Sort parameters by key
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        
        return sortedParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    public static long getTimestamp() {
        return System.currentTimeMillis();
    }
}

