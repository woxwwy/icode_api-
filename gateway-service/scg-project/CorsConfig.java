//跨域配置类，允许前端5170端口网页跨域访问后端接口
package com.example.scg_project;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;//登记跨域规则的工具
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;//提供配置MVC的接口，可以通过实现这个接口来定制MVC的配置，比如跨域配置

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 处理所有请求
                .allowedOrigins("http://localhost:5170")  // 允许5170前端地址访问
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true) //允许前端给后端发送cookie
                .maxAge(3600);  // 1小时 内不需要再次检查
    }
}
