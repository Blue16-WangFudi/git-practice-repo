import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  stages: [
    { duration: '15s', target: 5 },
    { duration: '30s', target: 20 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(90)', 'p(99)', 'p(99.9)', 'count'],
}

export default function () {
  const response = http.get(`${__ENV.BASE_URL || 'http://localhost:8088'}/api/v1/metrics/overview`)
  check(response, {
    'overview returns 200': (result) => result.status === 200,
    'overview returns json': (result) => result.headers['Content-Type']?.includes('application/json'),
  })
  sleep(1)
}

