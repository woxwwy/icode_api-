import http from 'k6/http';
import { check } from 'k6';

export default function () {
  const res = http.get('http://localhost:8081/api/light');
  
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });
}

export const options = {
  stages: [
    { duration: '10s', target: 2000 },
    { duration: '30s', target: 2000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.1'],
    http_req_duration: ['p(99)<2000'],
  },
};