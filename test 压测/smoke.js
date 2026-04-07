import http from 'k6/http';
import { check, sleep } from 'k6';

export default function () {
  // 测试三个接口
  const lightRes = http.get('http://localhost:8081/api/light');
  check(lightRes, {
    'light status is 200': (r) => r.status === 200,
  });
  
  const heavyRes = http.get('http://localhost:8081/api/heavy');
  check(heavyRes, {
    'heavy status is 200': (r) => r.status === 200,
  });
  
  const errorRes = http.get('http://localhost:8081/api/error');
  check(errorRes, {
    'error status is either 200 or 500': (r) => r.status === 200 || r.status === 500,
  });
  
  sleep(1);
}

export const options = {
  vus: 10,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(99)<500'],
  },
};