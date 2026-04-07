package com.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Component
public class SlidingWindow {
    
    @Value("${ratelimit.limit:100}")
    private int limit;
    
    @Value("${ratelimit.windowMs:1000}")
    private long windowMs;
    
    // 每个ip存一个请求时间队列
    private Map<String, Queue<Long>> ipWindows = new HashMap<>();
    
    public boolean allow(String ip) {
        System.out.println("=== SlidingWindow.allow() 被调用 ===");
        
        // 防止空指针，给个默认ip
        if (ip == null || ip.equals("")) {
            ip = "0.0.0.0";
        }
        
        long now = System.currentTimeMillis();
        
        // 对整个map加锁
        synchronized (ipWindows) {
            // 拿到这个ip的队列，如果没有就新建
            Queue<Long> timeList = ipWindows.get(ip);
            if (timeList == null) {
                timeList = new LinkedList<>();
                ipWindows.put(ip, timeList);
            }
            
            // 把超时的旧记录清掉
            while (!timeList.isEmpty()) {
                Long oldTime = timeList.peek();
                if (now - oldTime > windowMs) {
                    timeList.poll();  // 扔掉过期的
                } else {
                    break;  // 遇到没过期的，后面的都不用看了
                }
            }
            
            // 看看窗口里还剩多少请求
            if (timeList.size() < limit) {
                timeList.add(now);  // 把现在这条记进去
                System.out.println("结果: 允许访问  (当前窗口: " + (timeList.size()) + "/" + limit + ")");
                return true;        // 放行
            } else {
                System.out.println("结果: 限流  (当前窗口: " + timeList.size() + "/" + limit + ")");
                return false;       // 窗口满了，拒绝
            }
        }
    }
}