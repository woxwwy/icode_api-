package com.example.scg_project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "ping failed")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}