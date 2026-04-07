package com.gateway.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    
    private static final Logger log = LoggerFactory.getLogger(TokenBucketRedis.class);
    
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
        log.info("[限流器初始化] 令牌桶容量={}, 窗口={}ms", capacity, windowMs);  
    }
    
    @Override
    public Mono<Boolean> allow(String key) {
        long now = System.currentTimeMillis();
        long rate = capacity * 1000L / windowMs;
        
         
        log.info("[限流请求] key={}, 时间戳={}, 速率={}/s", key, now, rate);
        
        return redisTemplate.execute(
            redisScript,
            Collections.singletonList(key),
            String.valueOf(capacity),
            String.valueOf(rate),
            String.valueOf(now),
            "1"
        ).next()
         
         .doOnNext(result -> {
             if (result == 1) {
                 log.info("[限流通过] key={}", key);
             } else {
                 log.warn("[限流拒绝] key={}", key);  // 拒绝用warn级别，更醒目
             }
         })
         
         .doOnError(error -> log.error("[限流异常] key={}, 错误={}", key, error.getMessage()))
         .map(result -> result != null && result == 1);
    }
}
