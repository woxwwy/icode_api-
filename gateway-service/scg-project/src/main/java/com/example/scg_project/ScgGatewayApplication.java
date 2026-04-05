package com.example.scg_project;

import org.springframework.boot.SpringApplication;//启动spring程序的工具
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScgGatewayApplication {

    //java程序的入口，main方法是程序的起点，运行启动整个spring应用
    public static void main(String[] args) {
        SpringApplication.run(ScgGatewayApplication.class, args);
    }
}
