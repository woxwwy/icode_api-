package com.example.scg_project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/demo") //前端访问的路径
@CrossOrigin(origins = "http://localhost:5170")
public class DemoController {
    
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    
    @Autowired
    private RestTemplate restTemplate; // HTTP客户端，用于调用其他服务
    
    @GetMapping("/delivery")
    public Map<String, Object> deliveryService() {
        log.info("=== 转发渠道一：外卖服务被调用 ===");
        
        // 关键：网关转发请求到 user-service (8082)
        String userServiceResponse = restTemplate.getForObject(
            "http://localhost:8082/api/user/test", 
            String.class
        );
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("channel", "外卖服务");
        response.put("status", "转发成功");
        response.put("userServiceResponse", userServiceResponse);
        response.put("deliveryTime", "30分钟内送达");
        response.put("timestamp", new Date().toString());
        
        return response;
    }
    
    @GetMapping("/express")
    public Map<String, Object> expressService() {
        log.info("=== 转发渠道二：快递服务被调用 ===");
        
        String userServiceResponse = restTemplate.getForObject(
            "http://localhost:8082/api/light", 
            String.class
        );
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("channel", "快递服务");
        response.put("status", "转发成功");
        response.put("userServiceResponse", userServiceResponse);
        response.put("estimatedArrival", "预计明天送达");
        response.put("timestamp", new Date().toString());
        
        return response;
    }
}
