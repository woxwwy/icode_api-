package com.example.scg_project.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LogConfig implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        // 创建logs目录
        Path logDir = Paths.get("./logs");
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
            System.out.println("✅ 日志目录已创建: " + logDir.toAbsolutePath());
        }
        
        // 输出日志文件路径
        Path logFile = Paths.get("./logs/gateway.log");
        System.out.println("📝 日志文件路径: " + logFile.toAbsolutePath());
    }
}
