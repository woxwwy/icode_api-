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

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("=== RateLimitFilter 被调用了 ===");
        
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        if (ip == null) {
            ip = "unknown";
        }
        
        System.out.println("请求IP: " + ip);
        
        return rateLimiter.allow(ip).flatMap(allowed -> {
            if (!allowed) {
                System.out.println(">>> 触发限流！返回 429 <<<");
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        });
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
