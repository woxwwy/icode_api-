package com.gateway.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FallbackUtil {
    
    /**
     * 执行带降级的操作
     * @param operation 真正要执行的操作（比如调用user-service）
     * @param fallback 降级时的默认值
     * @param operationName 操作名称（用于日志）
     * @return 成功时返回operation结果，失败时返回fallback
     */
    public static <T> T executeWithFallback(
            java.util.function.Supplier<T> operation, 
            T fallback, 
            String operationName) {
        try {
            T result = operation.get();
            log.info("{} 成功，返回: {}", operationName, result);
            return result;
        } catch (Exception e) {
            log.error("{} 失败，原因: {}，触发降级，返回默认值: {}", 
                operationName, e.getMessage(), fallback);
            return fallback;
        }
    }
}