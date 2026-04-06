package com.gateway.limiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Collections;

@Component
public class TokenBucketRedis implements RateLimiter {
    
    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Value("${ratelimit.limit:10}")
    private int capacity;
    
    @Value("${ratelimit.windowMs:1000}")
    private long windowMs;
    
    private RedisScript<Long> redisScript;
    
    @PostConstruct
    public void init() {
        redisScript = RedisScript.of(new ClassPathResource("scripts/rate_limiter.lua"), Long.class);
    }
    
    @Override
    public Mono<Boolean> allow(String key) {
        long now = System.currentTimeMillis();
        long rate = capacity * 1000L / windowMs;
        
        return redisTemplate.execute(
            redisScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(rate),
            String.valueOf(now),
            "1"
        ).next().map(result -> result != null && result == 1);
    }
}