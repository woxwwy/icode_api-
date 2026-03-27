package com.gateway.filter;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    // 等组长写完 SlidingWindowRateLimiter 类后，取消下面的注释
    //@Autowired
    //private SlidingWindowRateLimiter limiter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = extractKey(exchange);
        
        // 这一行现在注释掉，用mock逻辑代替，等算法写完解注释
        // boolean allowed = limiter.allow(key);
        boolean allowed = mockAllow();
        
        if (!allowed) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
    
    private String extractKey(ServerWebExchange exchange) {
        // 获取客户端IP地址
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return ip;
    }
    
    private boolean mockAllow() {
        // 临时mock方法，等限流算法实现后删除
        return true;
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}