package com.example.scg_project.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5170")  // 允许前端地址跨域访问
public class RedisPingController {

    @Autowired
    private JedisPool jedisPool;

    @GetMapping("/ping")
    public String ping() {
        if (jedisPool == null) {
            log.error("❌ JedisPool is null - 配置可能有问题，降级放行");
            return "true";
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.ping();

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
}
