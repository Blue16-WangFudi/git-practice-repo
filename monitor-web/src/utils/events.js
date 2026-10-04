function validateDelay(delay) {
  if (!Number.isFinite(delay) || delay < 0) throw new TypeError('Delay must be a non-negative finite number')
}

// Trailing debounce: wait for a quiet period and use the latest arguments.
export function debounce(callback, delay = 300, clock = globalThis) {
  validateDelay(delay)
  let timer
  function wrapped(...args) {
    if (timer !== undefined) clock.clearTimeout(timer)
    const receiver = this
    timer = clock.setTimeout(() => {
      timer = undefined
      callback.apply(receiver, args)
    }, delay)
  }
  wrapped.cancel = () => {
    if (timer !== undefined) clock.clearTimeout(timer)
    timer = undefined
  }
  return wrapped
}

// Leading + trailing throttle: run immediately, then keep only the latest event.
export function throttle(callback, interval = 150, clock = globalThis) {
  validateDelay(interval)
  let timer
  let pendingArgs
  let pendingReceiver
  function invoke(receiver, args) {
    timer = clock.setTimeout(() => {
      timer = undefined
      if (pendingArgs) {
        const nextArgs = pendingArgs
        const nextReceiver = pendingReceiver
        pendingArgs = pendingReceiver = undefined
        invoke(nextReceiver, nextArgs)
      }
    }, interval)
    callback.apply(receiver, args)
  }
  function wrapped(...args) {
    if (timer !== undefined) {
      pendingArgs = args
      pendingReceiver = this
    } else {
      invoke(this, args)
    }
  }
  wrapped.cancel = () => {
    if (timer !== undefined) clock.clearTimeout(timer)
    timer = pendingArgs = pendingReceiver = undefined
  }
  return wrapped
}

export function filterServices(services, query) {
  const needle = String(query ?? '').trim().toLocaleLowerCase()
  if (!needle) return services
  return services.filter((service) =>
    ['name', 'serviceKey', 'groupName', 'endpointUrl'].some((field) =>
      String(service[field] ?? '').toLocaleLowerCase().includes(needle)))
}
