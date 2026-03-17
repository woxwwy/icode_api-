//检查redis连接是否正常

package com.example.scg_project.controller;

import lombok.extern.slf4j.Slf4j;//写日志
//spring框架的各种功能
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j //自动创建一个log对象，使用log.info()、log.error()方法写日志,打印日志，如控制台输出
@RestController //处理HTTP请求，并返回数据
@CrossOrigin(origins = "http://localhost:5170")  // 允许5170前端网页 跨域访问后端接口
public class RedisPingController {

    @Autowired //spring自动注入配好的jedispool对象，使用之前配置的连接池
    private JedisPool jedisPool;

    //核心方法
    @GetMapping("/ping")
    public String ping() {
        //检查连接池是否可用，如果不可用，记录错误日志并返回降级结果true，表示虽然redis不可用，但接口仍然可用，继续执行
        if (jedisPool == null) {
            log.error("❌ JedisPool is null - 配置可能有问题，降级放行");
            return "true";
        }

        try (Jedis jedis = jedisPool.getResource()) { //从连接池获取一个jedis连接，try-with-resources语法，确保使用完后自动关闭连接，避免资源泄漏
            String result = jedis.ping();//向redis服务器发送ping命令，检查连接是否正常，如果返回PONG，说明连接正常，否则说明连接异常，返回降级结果true

            if ("PONG".equals(result)) {
                log.info("✅ Redis PING 成功: {}", result);
                return result;
            } else {
                log.warn("⚠️ Redis PING 返回异常结果: {}, 降级放行", result);
                return "true";
            }

        } catch (Exception e) {
            log.error("❌ Redis 连接失败，降级放行", e);
            return "true";
        }
    }
}
