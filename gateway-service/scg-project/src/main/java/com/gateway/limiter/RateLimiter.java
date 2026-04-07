//创建RateLimiter.java 接口
package com.gateway.limiter;

import reactor.core.publisher.Mono;

public interface RateLimiter {
    Mono<Boolean> allow(String key);
}