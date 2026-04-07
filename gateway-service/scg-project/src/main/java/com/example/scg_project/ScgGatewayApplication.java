package com.example.scg_project;

import org.springframework.boot.SpringApplication;//启动spring程序的工具
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jmx.support.RegistrationPolicy;


@SpringBootApplication
@ComponentScan(basePackages = {"com.example.scg_project", "com.gateway"}) // 这行告诉Spring去扫描com.gateway包，这样我们写的RateLimitFilter才能被Spring发现并注册到容器中

@org.springframework.context.annotation.EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)

public class ScgGatewayApplication {

    //java程序的入口，main方法是程序的起点，运行启动整个spring应用
    public static void main(String[] args) {
        SpringApplication.run(ScgGatewayApplication.class, args);
    }
}
