# API Gateway Project
# API网关项目README.md


# API网关
本项目是一个功能完善的API网关实现，集成请求转发、高可用限流、实时监控及全场景压测能力，适用于微服务架构下的接口统一管理场景。


## 项目作品概述
核心功能覆盖：请求转发 + 爽算法限流（滑动窗口java、令牌桶+Lua+Redis）+ 实时监控仪表板 + K6全场景压测


## 核心亮点
- ① 动态路由转发：基于Spring Cloud Gateway，支持多服务路由配置
- ② 双限流算法：纯java滑动窗口、令牌桶算法（Lua+Redis原子操作，生产级性能）
- ③ 实时监控面板：Vue3 + ECharts，展示QPS、限流次数、响应延迟
- ④ 全场景压测：K6脚本（负载/冒烟/尖峰/基准测试）+ 测试报告示例
- ⑤ 部署简单：Docker一键启动Redis，Maven打包运行


## 技术栈
| 模块 | 技术 |
| ---- | ---- |
| 网关核心 | Spring Cloud Gateway + Netty |
| 限流 | Redis + Lua（令牌桶） |
| 监控 | Vue3 + TypeScript + ECharts |
| 压测 | K6 |
| 构建 | Maven + NPM |


## 复现操作
> 需提前装好Java17、Maven、Docker、Node.js
### 1. 启动Redis
```bash
docker run -d --name redis-scg -p 6379:6379 redis:alpine
```

### 2. 启动网关服务
```bash
cd gateway-service/scg-project
mvn spring-boot:run
```

### 3. 启动监控面板（新开终端）
```bash
cd monitor-dashboard
npm install
npm run dev
# 访问 http://localhost:5170
```

### 4. 测试转发+限流
网关端口为 8081，已配置以下路由转发规则：

| 路由名称 | 路径匹配规则 | 转发目标地址 |
|---------|-------------|-------------|
| user-service-route | /api/** | http://localhost:8082 |
| order-service-route | /api/order/** | http://localhost:8083 |
| product-service-route | /api/product/** | http://localhost:8084 |
#### ① 测试 light 接口（快速响应）
```bash
curl http://localhost:8081/api/light
```
**预期返回**：
Light response from user-service: 1734567890123

#### ② 测试 heavy 接口（模拟延迟）
```bash
curl http://localhost:8081/api/heavy
```
**预期返回（约100ms后）**：
Heavy response from user-service (delayed 100ms): 1734567890123

#### ③ 测试 error 接口（随机错误）
```bash
curl http://localhost:8081/api/error
```
**预期**： 50%概率返回成功，50%概率返回500错误。

#### ④ 测试限流接口
```bash
curl -X POST http://localhost:8081/api/test-rate-limit
```
**预期返回**： OK
限流功能可视化测试：
浏览器访问 http://localhost:5170/test-limit 查看实时限流效果。
换限流算法时只需注释掉另外一种算法即可。

## 压测验证
使用 K6 进行令牌桶限流压测，验证高并发下的限流稳定性。
### 测试配置
- 场景：固定速率 1689 req/s 持续冲击网关
- 下游服务：Mock接口（延迟50ms）
- 限流阈值：令牌桶容量500，填充速率100/s

### 关键指标
| 指标 | 数值 | 说明 |
|------|------|------|
| K6发送速率 | 1689 req/s | 压测端恒定输出 |
| 系统实际吞吐 | ~100 req/s | 受限于令牌桶填充速率（符合预期） |
| P99延迟 | 375.87ms | 排队等待令牌 + 网络开销 |
| 限流拒绝率 | 99.70% | 超额请求被精准拦截（核心验证目标） |
| 总请求数 | 169,017 | 持续压测约100秒 |

### 结论
① 限流生效：99.7%超额请求被正确拒绝，仅~0.3%穿透（系统误差范围内）  
② 自我保护：网关CPU稳定在45%，内存无泄漏，未出现级联故障  
③ 延迟偏高：375ms P99源于Redis Lua执行+令牌排队，建议生产环境启用本地缓存优化
> 本测试故意超载以验证限流算法可靠性，非性能基准测试。正常负载下（<100 req/s）P99延迟约45ms。


## 项目结构
iCode-api-gateway/
├── gateway-core/          # 限流算法核心
│  ├── SlidingWindow.java      # 滑动窗口纯Java实现
│  └── rate_limiter.lua   # 令牌桶Redis脚本
├── gateway-service/       # SCG网关服务
├── monitor-dashboard/     # Vue3监控面板
└── test压测/              # K6测试套件
   ├── load.js            # 负载测试
   ├── smoke.js           # 冒烟测试
   ├── k6脚本.js           #基准测试 
   └── spike.js           # 尖峰测试



## 团队分工
| 成员 | 负责模块 |
|------|---------|
| 林怡菲| 网关核心 + 限流算法实现 |
| 宁伟琛| K6压测方案 + 辅助调整性能 |
| 张颖涵| Vue3监控面板 + ECharts可视化 |
