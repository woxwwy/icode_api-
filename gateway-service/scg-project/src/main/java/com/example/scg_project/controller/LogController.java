package com.example.scg_project.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/logs")
@CrossOrigin(origins = "http://localhost:5170")
public class LogController {

    @GetMapping("/recent")
    public List<String> getRecentLogs() {
        List<String> result = new ArrayList<>();
        try {
            String logPath = System.getProperty("user.dir") + "/logs/gateway.log";
            Path path = Paths.get(logPath);
            
            // 如果文件不存在，创建示例日志
            if (!Files.exists(path)) {
                result.add("日志文件不存在，请先发起请求触发限流");
                result.add("示例: curl http://localhost:8081/api/user/test");
                return result;
            }
            
            // 检查文件大小，如果为空则返回提示
            if (Files.size(path) == 0) {
                result.add("日志文件为空，请先发起请求触发限流");
                result.add("示例: curl http://localhost:8081/api/user/test");
                return result;
            }
            
            // 读取文件内容
            List<String> allLines = readFileWithMultipleEncodings(path);
            
            if (allLines == null || allLines.isEmpty()) {
                result.add("日志文件为空，请先发起请求触发限流");
                return result;
            }
            
            // 过滤掉配置警告和启动信息，只保留限流相关日志
            List<String> filteredLines = new ArrayList<>();
            for (String line : allLines) {
                if (line.contains("RateLimitFilter") || 
                    line.contains("TokenBucketRedis") ||
                    line.contains("限流") ||
                    line.contains("收到请求")) {
                    filteredLines.add(line);
                }
            }
            
            // 如果没有限流日志，显示提示
            if (filteredLines.isEmpty()) {
                result.add("暂无限流日志，请先发起请求触发限流");
                result.add("快速执行: 1..5 | ForEach-Object { curl http://localhost:8081/api/user/test }");
                return result;
            }
            
            int total = filteredLines.size();
            int start = Math.max(0, total - 50);
            return new ArrayList<>(filteredLines.subList(start, total));
            
        } catch (Exception e) {
            result.add("读取日志失败: " + e.getMessage());
            result.add("请确保已触发限流产生日志");
            e.printStackTrace();
            return result;
        }
    }
    
    /**
     * 尝试多种编码读取文件
     */
    private List<String> readFileWithMultipleEncodings(Path path) throws IOException {
        // 按优先级尝试的编码
        Charset[] charsets = {
            StandardCharsets.UTF_8,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.defaultCharset()
        };
        
        for (Charset charset : charsets) {
            try {
                List<String> lines = Files.readAllLines(path, charset);
                if (lines != null && !lines.isEmpty()) {
                    return lines;
                }
            } catch (Exception e) {
                // 继续尝试下一个编码
            }
        }
        
        // 如果都失败，返回空列表
        return new ArrayList<>();
    }
}
