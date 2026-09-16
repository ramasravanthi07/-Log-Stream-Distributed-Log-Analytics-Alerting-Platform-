const base = '/api'

async function json(res) {
  const body = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(body.error || `Request failed with ${res.status}`)
  return body
}

export function search({ q, from = 0, size = 50, buckets = 60 }) {
  const params = new URLSearchParams({ q, from, size, buckets, histogram: 'true' })
  return fetch(`${base}/search?${params}`).then(json)
}

export function validate(q) {
  return fetch(`${base}/validate?q=${encodeURIComponent(q)}`).then(json)
}

export function stats() {
  return fetch(`${base}/stats`).then(json)
}

export function listAlerts() {
  return fetch(`${base}/alerts`).then(json)
}

export function createAlert(rule) {
  return fetch(`${base}/alerts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(rule)
  }).then(json)
}

export function deleteAlert(id) {
  return fetch(`${base}/alerts/${id}`, { method: 'DELETE' }).then(json)
}

export function evaluateAlert(id) {
  return fetch(`${base}/alerts/${id}/evaluate`, { method: 'POST' }).then(json)
}
