
//检查redis连接是否正常

package com.example.scg_project.controller;

import lombok.extern.slf4j.Slf4j;//写日志
//spring框架的各种功能
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;


@Slf4j //自动创建一个log对象，使用log.info()、log.error()方法写日志,打印日志，如控制台输出
@RestController //处理HTTP请求，并返回数据
@CrossOrigin(origins = "http://localhost:5170")  // 允许5170前端网页 跨域访问后端接口
public class RedisPingController {

    @Autowired //spring自动注入配好的jedispool对象，使用之前配置的连接池
    private JedisPool jedisPool;

    //核心方法
    @GetMapping("/redis/ping")
    public String redisPing() {
        //检查连接池是否可用，如果不可用，记录错误日志并返回降级结果true，表示虽然redis不可用，但接口仍然可用，继续执行
        if (jedisPool == null) {
            log.error("❌ JedisPool is null - 配置可能有问题，降级放行");
            return "true";
        }

        try (Jedis jedis = jedisPool.getResource()) { //从连接池获取一个jedis连接，try-with-resources语法，确保使用完后自动关闭连接，避免资源泄漏
            String result = jedis.ping();//向redis服务器发送ping命令，检查连接是否正常，如果返回PONG，说明连接正常，否则说明连接异常，返回降级结果true

            if ("PONG".equals(result)) {
                log.info("✅ Redis PING 成功: {}", result);
                return result;
            } else {
                log.warn("⚠️ Redis PING 返回异常结果: {}, 降级放行", result);
                return "true";
            }

        } catch (Exception e) {
            log.error("❌ Redis 连接失败，降级放行", e);
            return "true";
        }
    }
    // 新增：返回结构化 JSON 的 Redis 状态接口（供前端“Redis连通性测试”使用）
    @GetMapping("/api/gateway/redis/status")
    public Map<String, Object> redisStatus() {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();
        try {
            if (jedisPool == null) {
                result.put("connected", false);
                result.put("error", "JedisPool未初始化");
                result.put("latency", System.currentTimeMillis() - start);
                return result;
            }
            try (Jedis jedis = jedisPool.getResource()) {
                String pong = jedis.ping();
                boolean connected = "PONG".equals(pong);
                result.put("connected", connected);
                result.put("error", connected ? null : "Redis返回非PONG");
                result.put("latency", System.currentTimeMillis() - start);
                if (connected) {
                    log.info("Redis状态检查成功，延迟: {}ms", result.get("latency"));
                } else {
                    log.warn("Redis状态检查失败，返回: {}", pong);
                }
            }
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("latency", System.currentTimeMillis() - start);
            log.error("Redis状态检查异常", e);
        }
        return result;
    }    
}
