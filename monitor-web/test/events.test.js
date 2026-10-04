import assert from 'node:assert/strict'
import test from 'node:test'
import { debounce, filterServices, throttle } from '../src/utils/events.js'

function fakeClock() {
  let now = 0
  let nextId = 0
  const timers = new Map()
  return {
    get now() { return now },
    get pending() { return timers.size },
    setTimeout(callback, delay) {
      const id = nextId++ // Include ID 0 to catch truthiness bugs.
      timers.set(id, { at: now + delay, callback })
      return id
    },
    clearTimeout(id) { timers.delete(id) },
    advance(milliseconds) {
      const end = now + milliseconds
      while (true) {
        const next = [...timers.entries()].filter(([, timer]) => timer.at <= end)
          .sort((a, b) => a[1].at - b[1].at || a[0] - b[0])[0]
        if (!next) break
        now = next[1].at
        timers.delete(next[0])
        next[1].callback()
      }
      now = end
    },
  }
}

test('debounce waits for the quiet period and uses latest input', () => {
  const clock = fakeClock()
  const calls = []
  const search = debounce((value) => calls.push([clock.now, value]), 300, clock)
  search('r')
  clock.advance(100)
  search('redis')
  clock.advance(299)
  assert.deepEqual(calls, [])
  clock.advance(1)
  assert.deepEqual(calls, [[400, 'redis']])
  assert.equal(clock.pending, 0)
})

test('debounce cancel prevents an unmounted component update and permits reuse', () => {
  const clock = fakeClock()
  const calls = []
  const update = debounce((value) => calls.push(value), 10, clock)
  update('old')
  update.cancel()
  clock.advance(10)
  assert.deepEqual(calls, [])
  update('new')
  clock.advance(10)
  assert.deepEqual(calls, ['new'])
})

test('debounce preserves caller context and all arguments', () => {
  const clock = fakeClock()
  const target = { value: 0, update: debounce(function (a, b) { this.value = a + b }, 10, clock) }
  target.update(2, 3)
  clock.advance(10)
  assert.equal(target.value, 5)
})

test('throttle runs leading and the latest trailing event at bounded intervals', () => {
  const clock = fakeClock()
  const calls = []
  const scroll = throttle((value) => calls.push([clock.now, value]), 150, clock)
  scroll(0)
  clock.advance(50)
  scroll(20)
  clock.advance(50)
  scroll(80)
  clock.advance(50)
  assert.deepEqual(calls, [[0, 0], [150, 80]])
  scroll(100)
  clock.advance(150)
  assert.deepEqual(calls, [[0, 0], [150, 80], [300, 100]])
  clock.advance(150)
  assert.equal(clock.pending, 0)
})

test('throttle cancel discards pending events and resets the gate', () => {
  const clock = fakeClock()
  const calls = []
  const update = throttle((value) => calls.push(value), 150, clock)
  update(1)
  update(2)
  update.cancel()
  assert.equal(clock.pending, 0)
  clock.advance(500)
  update(3)
  assert.deepEqual(calls, [1, 3])
})

test('throttle trailing event preserves the latest receiver', () => {
  const clock = fakeClock()
  const fn = throttle(function (value) { this.value = value }, 100, clock)
  const first = { value: 0 }
  const last = { value: 0 }
  fn.call(first, 1)
  fn.call(last, 2)
  clock.advance(100)
  assert.equal(first.value, 1)
  assert.equal(last.value, 2)
})

test('invalid delays are rejected', () => {
  for (const wrapper of [debounce, throttle]) {
    for (const delay of [-1, NaN, Infinity, '300']) {
      assert.throws(() => wrapper(() => {}, delay), TypeError)
    }
  }
})

test('service search matches names, keys, groups and URLs without regular expressions', () => {
  const services = [
    { name: '缓存服务', serviceKey: 'REDIS', groupName: 'Data', endpointUrl: 'https://api.example.com' },
    { name: '邮件', serviceKey: 'mail', groupName: 'Notice', endpointUrl: null },
    { name: '[literal]' },
  ]
  assert.deepEqual(filterServices(services, ' 缓存 '), [services[0]])
  assert.deepEqual(filterServices(services, 'redis'), [services[0]])
  assert.deepEqual(filterServices(services, 'notice'), [services[1]])
  assert.deepEqual(filterServices(services, 'API.example'), [services[0]])
  assert.deepEqual(filterServices(services, '['), [services[2]])
  assert.deepEqual(filterServices(services, 'missing'), [])
  assert.equal(filterServices(services, ''), services)
})
