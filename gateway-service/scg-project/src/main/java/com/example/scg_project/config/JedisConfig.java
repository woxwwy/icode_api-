//配置java的jedis连接池，连接redis数据库

//确认文件所在地址
package com.example.scg_project.config;

//spring框架注释
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//jedis用来连接和管理redis的类
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

//读取配置文件中的参数，提供默认值，方便在没有配置文件的情况下也能运行
@Configuration
public class JedisConfig {
    //redis服务器地址
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:5000}")
    private int redisTimeout;

    //创建连接池配置对象（设计图）
    @Value("${spring.data.redis.jedis.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.data.redis.jedis.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.data.redis.jedis.pool.min-idle:0}")
    private int minIdle;

    @Value("${spring.data.redis.jedis.pool.max-wait:-1}")
    private long maxWait;

    //创建jedis连接池配置对象
    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWaitMillis(maxWait);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        return poolConfig;
    }

    //创建jedis连接池对象，使用上面创建的连接池配置对象，并设置连接参数
    @Bean
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {  //密码存在且不为空
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, redisPassword, redisDatabase);
        } else {//创建不带密码的连接池
            return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, null, redisDatabase);
        }
    }
}
