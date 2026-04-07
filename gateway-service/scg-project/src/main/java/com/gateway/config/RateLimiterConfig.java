package com.gateway.config;

//import com.gateway.limiter.SlidingWindowHashMap;
import com.gateway.limiter.TokenBucketRedis;
import com.gateway.limiter.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {
    
    /*
     * 版本1：单机HashMap版（一开始写的那个滑动窗口旧版）
     */
    // @Bean
    // public RateLimiter rateLimiter() {
    //     return new SlidingWindowHashMap();
    // }
    
    /*
     * 版本2：Redis令牌桶版（新版，分布式优化）
     */
    @Bean
    public RateLimiter rateLimiter() {
        return new TokenBucketRedis();
    }
}