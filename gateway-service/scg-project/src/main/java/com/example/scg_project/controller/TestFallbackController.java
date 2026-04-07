package com.example.scg_project.controller;

import com.gateway.util.FallbackUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestFallbackController {
    
    @GetMapping("/test-with-fallback")
    public Map<String, String> testWithFallback() {
        // 准备Mock数据（降级时返回的默认值）
        Map<String, String> mockData = Map.of(
            "id", "fallback-route",
            "uri", "http://mock-service:9999", 
            "message", "真实服务挂了，这是降级数据"
        );
        
        // 尝试调用真实的user-service（现在模拟失败）
        return FallbackUtil.executeWithFallback(
            () -> {
                // 模拟调用真实服务失败
                throw new RuntimeException("模拟：user-service挂了");
            },
            mockData,  // 失败时返回这个
            "调用user-service"
        );
    }
}