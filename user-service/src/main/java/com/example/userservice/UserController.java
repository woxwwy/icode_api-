package com.example.userservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")  // 允许所有前端跨域访问
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final Random random = new Random();

    // 原来的测试接口（保留）
    @GetMapping("/")
    public String test() {
        return "user-service-response";
    }
    
    // 可以访问 http://localhost:8082/test 看 user-service 是否正常响应
    @GetMapping("/test")
    public String simpleTest() {
        return "Simple test works!";
    }
    
    // 新接口：轻量业务接口（快速返回）
    @GetMapping("/api/light")
    public String light() {
        logger.info("调用轻量业务接口 /api/light");
        return "Light response from user-service: " + System.currentTimeMillis();
    }
    
    // 新接口：耗时业务接口（模拟100ms延迟）
    @GetMapping("/api/heavy")
    public String heavy() throws InterruptedException {
        logger.info("调用耗时业务接口 /api/heavy，开始处理...");
        Thread.sleep(100); // 模拟耗时操作100ms
        logger.info("耗时业务接口 /api/heavy 处理完成");
        return "Heavy response from user-service (delayed 100ms): " + System.currentTimeMillis();
    }
    
    // 新接口：错误测试接口（50%概率报错）
    @GetMapping("/api/error")
    public String error() {
        logger.info("调用错误测试接口 /api/error");
        
        // 50% 概率抛出异常
        if (random.nextBoolean()) {
            logger.error("触发随机异常！");
            throw new RuntimeException("Simulated random error (50% probability)");
        }
        
        return "Success response from user-service: " + System.currentTimeMillis();
    }
    
    // 原有的接口（保留）
    @GetMapping("/api/user/test")
    public String userTest() {
        return "User Service Response - 访问时间: " + java.time.LocalDateTime.now();
    }
    
    // 提供给前端的路由数据接口（保留）
    @GetMapping("/api/routes")
    public List<Map<String, Object>> getRoutes() {
        List<Map<String, Object>> routes = new ArrayList<>();
        
        // 第1条路由：用户服务
        Map<String, Object> route1 = new HashMap<>();
        route1.put("id", "user-service-route");
        route1.put("path", "/api/user/**");
        route1.put("uri", "http://localhost:8082");
        route1.put("component", "user-service");
        route1.put("name", "用户服务路由");
        route1.put("status", "normal");
        routes.add(route1);
        
        // 第2条路由
        Map<String, Object> route2 = new HashMap<>();
        route2.put("id", "order-service-route");
        route2.put("path", "/api/order/**");
        route2.put("uri", "http://localhost:8083");
        route2.put("component", "order-service");
        route2.put("name", "订单服务路由");
        route2.put("status", "normal");
        routes.add(route2);
        
        // 第3条路由
        Map<String, Object> route3 = new HashMap<>();
        route3.put("id", "product-service-route");
        route3.put("path", "/api/product/**");
        route3.put("uri", "http://localhost:8084");
        route3.put("component", "product-service");
        route3.put("name", "商品服务路由");
        route3.put("status", "normal");
        routes.add(route3);
        
        return routes;
    }
}