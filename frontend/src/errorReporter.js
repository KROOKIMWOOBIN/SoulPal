const send = (payload) => {
  // 토큰이 있으면 JWT 포함, 없어도 전송
  const token = localStorage.getItem('soulpal_token')
  fetch('/api/logs/error', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload)
  }).catch(() => {}) // 리포터 자체 에러는 무시
}

// Vue 전역 에러 핸들러로 등록
export function setupErrorReporter(app) {
  app.config.errorHandler = (err, _instance, info) => {
    console.error('[Vue Error]', err)
    send({
      message: err?.message || String(err),
      stack: err?.stack || '',
      source: `Vue: ${info}`,
      route: window.location.pathname,
      userAgent: navigator.userAgent
    })
  }

  // 처리되지 않은 Promise rejection
  window.addEventListener('unhandledrejection', (event) => {
    const err = event.reason
    send({
      message: err?.message || String(err),
      stack: err?.stack || '',
      source: 'unhandledrejection',
      route: window.location.pathname,
      userAgent: navigator.userAgent
    })
  })

  // 일반 JS 에러
  window.addEventListener('error', (event) => {
    send({
      message: event.message,
      stack: event.error?.stack || '',
      source: `${event.filename}:${event.lineno}:${event.colno}`,
      route: window.location.pathname,
      userAgent: navigator.userAgent
    })
  })
}
