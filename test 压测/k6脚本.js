import http from 'k6/http';
import { check, sleep } from 'k6';

// 通过命令行传入，确保后面我改的参数和一开始第一版算法的完全一致（控制变量）
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081'; //测网关
const MAX_VUS = parseInt(__ENV.VUS) || 1000;      // 最高并发数，建议Day 10/11都用1000
const TEST_DURATION = __ENV.DURATION || '2m';     // 主力测试时长，建议2分钟取平均

export const options = {
  // 简化阶段：稳定爬坡 → 持续高压 → 降速（去掉"突发"，确保数据稳定可比）
  stages: [
    { duration: '30s', target: MAX_VUS * 0.1 },  // 温和热身，让连接池建立
    { duration: TEST_DURATION, target: MAX_VUS }, // 稳定并发，采集QPS和延迟
    { duration: '20s', target: 0 },              // 逐渐降速，观察系统恢复
  ],
  
  thresholds: {
    // 特此声明：本项目测试95%请求模拟在500ms内（比p99稳定，不易受极端值干扰）
    http_req_duration: ['p(95)<500'], 
    
    // 系统错误率<5%（429限流不算错误，是预期保护行为）
    http_req_failed: ['rate<0.05'], 
  },
};

export default function () {
  // 单一接口（配合user-service的/test，无需额外实现慢接口）
  const res = http.get(`${BASE_URL}/test`, {
    headers: {
      // 保留请求ID，方便日志里追踪特定请求（出问题时可grep vu-编号）
      'X-Request-ID': `vu-${__VU}-time-${Date.now()}`,
    },
  });
  
  // 检查点简化：200是成功，429是限流生效（都算"正常响应"）
  check(res, {
    '服务正常或限流生效': (r) => r.status === 200 || r.status === 429,
    '无超时错误': (r) => r.status !== 0, // k6中status 0表示网络/超时错误
  });
  
  sleep(1);  }
