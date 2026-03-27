package com.example.scg_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Reactive 配置类
 * 用于限流算法的响应式Redis操作
 * 注意：此配置不影响原有的 JedisConfig
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 ReactiveRedisTemplate
     * 用于执行 Lua 脚本和响应式 Redis 操作
     * Spring Boot 会自动读取 application.yml 中的 redis 配置（包括 lettuce.pool）
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        // 使用字符串序列化器
        StringRedisSerializer serializer = new StringRedisSerializer();
        
        // 配置序列化上下文
        RedisSerializationContext<String, String> serializationContext = 
                RedisSerializationContext.<String, String>newSerializationContext(serializer)
                        .key(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}