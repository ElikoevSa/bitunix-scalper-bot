package com.bitunix.scalper.controller;

import com.bitunix.scalper.service.BybitDemoTradingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing Bybit Demo Trading
 */
@Controller
@RequestMapping("/demo")
public class DemoTradingController {
    
    @Autowired
    private BybitDemoTradingService demoTradingService;
    
    /**
     * Demo trading dashboard
     */
    @GetMapping("/dashboard")
    public String demoDashboard(Model model) {
        // Get account info
        JsonNode accountInfo = demoTradingService.getAccountInfo();
        if (accountInfo != null) {
            model.addAttribute("accountInfo", accountInfo);
        }
        
        // Get wallet balance
        JsonNode walletBalance = demoTradingService.getWalletBalance("UNIFIED");
        if (walletBalance != null) {
            model.addAttribute("walletBalance", walletBalance);
        }
        
        // Get open orders
        JsonNode openOrders = demoTradingService.getOpenOrders("linear", null);
        if (openOrders != null) {
            model.addAttribute("openOrders", openOrders);
        }
        
        // Get positions
        JsonNode positions = demoTradingService.getPositions("linear", null);
        if (positions != null) {
            model.addAttribute("positions", positions);
        }
        
        return "demo-dashboard";
    }
    
    /**
     * Request demo funds
     * POST /demo/funds/request
     */
    @PostMapping("/funds/request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestDemoFunds(
            @RequestParam(defaultValue = "0") int adjustType,
            @RequestParam String coin,
            @RequestParam String amount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, String> demoApplyMoney = new HashMap<>();
            demoApplyMoney.put(coin, amount);
            
            JsonNode result = demoTradingService.requestDemoFunds(adjustType, demoApplyMoney);
            
            if (result != null && result.has("retCode") && result.get("retCode").asInt() == 0) {
                response.put("success", true);
                response.put("message", "Demo funds requested successfully");
                response.put("data", result);
            } else {
                response.put("success", false);
                response.put("message", result != null && result.has("retMsg") ? 
                    result.get("retMsg").asText() : "Failed to request demo funds");
                response.put("data", result);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get wallet balance
     * GET /demo/balance
     */
    @GetMapping("/balance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBalance(
            @RequestParam(defaultValue = "UNIFIED") String accountType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode balance = demoTradingService.getWalletBalance(accountType);
            
            if (balance != null) {
                response.put("success", true);
                response.put("data", balance);
            } else {
                response.put("success", false);
                response.put("message", "Failed to get wallet balance");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Place order
     * POST /demo/order/place
     */
    @PostMapping("/order/place")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> placeOrder(
            @RequestParam String category,
            @RequestParam String symbol,
            @RequestParam String side,
            @RequestParam String orderType,
            @RequestParam String qty,
            @RequestParam(required = false) String price) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode result = demoTradingService.placeOrder(category, symbol, side, orderType, qty, price);
            
            if (result != null && result.has("retCode") && result.get("retCode").asInt() == 0) {
                response.put("success", true);
                response.put("message", "Order placed successfully");
                response.put("data", result);
            } else {
                response.put("success", false);
                response.put("message", result != null && result.has("retMsg") ? 
                    result.get("retMsg").asText() : "Failed to place order");
                response.put("data", result);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get open orders
     * GET /demo/orders/open
     */
    @GetMapping("/orders/open")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOpenOrders(
            @RequestParam String category,
            @RequestParam(required = false) String symbol) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode orders = demoTradingService.getOpenOrders(category, symbol);
            
            if (orders != null) {
                response.put("success", true);
                response.put("data", orders);
            } else {
                response.put("success", false);
                response.put("message", "Failed to get open orders");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel order
     * POST /demo/order/cancel
     */
    @PostMapping("/order/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @RequestParam String category,
            @RequestParam String symbol,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String orderLinkId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode result = demoTradingService.cancelOrder(category, symbol, orderId, orderLinkId);
            
            if (result != null && result.has("retCode") && result.get("retCode").asInt() == 0) {
                response.put("success", true);
                response.put("message", "Order cancelled successfully");
                response.put("data", result);
            } else {
                response.put("success", false);
                response.put("message", result != null && result.has("retMsg") ? 
                    result.get("retMsg").asText() : "Failed to cancel order");
                response.put("data", result);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get positions
     * GET /demo/positions
     */
    @GetMapping("/positions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPositions(
            @RequestParam String category,
            @RequestParam(required = false) String symbol) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode positions = demoTradingService.getPositions(category, symbol);
            
            if (positions != null) {
                response.put("success", true);
                response.put("data", positions);
            } else {
                response.put("success", false);
                response.put("message", "Failed to get positions");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get account info
     * GET /demo/account/info
     */
    @GetMapping("/account/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            JsonNode accountInfo = demoTradingService.getAccountInfo();
            
            if (accountInfo != null) {
                response.put("success", true);
                response.put("data", accountInfo);
            } else {
                response.put("success", false);
                response.put("message", "Failed to get account info");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

