import http from 'k6/http';
import { check, sleep } from 'k6';

export default function () {
  const res = http.get('http://localhost:8081/api/light');
  
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });
  
  if (res.status === 429) {
    console.log(`Rate limited at ${new Date().toISOString()}`);
  }
  
  sleep(0.1);
}

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '30s', target: 500 },
    { duration: '30s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(99)<1000'],
  },
};