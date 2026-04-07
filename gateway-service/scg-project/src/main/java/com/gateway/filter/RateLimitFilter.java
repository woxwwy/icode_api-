package com.gateway.filter;

import com.gateway.limiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("=== RateLimitFilter 被调用了 ===");
        
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        if (ip == null) {
            ip = "unknown";
        }
        
        // 修复：创建 final 或 effective final 的变量副本
        final String clientIp = ip;
        final String requestPath = exchange.getRequest().getURI().getPath();
        
        System.out.println("请求IP: " + clientIp);
        
        // 记录请求开始
        log.info("收到请求 - IP: {}, 路径: {}", clientIp, requestPath);
        
        return rateLimiter.allow(clientIp).flatMap(allowed -> {
            if (!allowed) {
                System.out.println(">>> 触发限流！返回 429 <<<");
                // 记录限流拒绝日志
                log.warn("限流拒绝 - IP: {}, 路径: {}, 状态: 429 TOO_MANY_REQUESTS", 
                         clientIp, requestPath);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            // 记录限流通过日志
            log.info("限流通过 - IP: {}, 路径: {}", clientIp, requestPath);
            return chain.filter(exchange);
        });
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
