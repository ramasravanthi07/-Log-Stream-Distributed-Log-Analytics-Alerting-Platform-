import { useEffect, useState } from 'react'
import { validate } from '../api'

const EXAMPLES = [
  'level:ERROR AND service:billing-api AND response_time > 1000',
  'level:ERROR AND timestamp >= now-15m',
  '(level:ERROR OR level:WARN) AND NOT service:search-api',
  'deadlock AND response_time > 2000',
  '"connection pool exhausted"',
  'status_code:500 AND service:payment-gateway'
]

export default function QueryBar({ value, onChange, onSubmit, busy }) {
  const [problem, setProblem] = useState(null)

  // Validate on the server after the user stops typing, so the grammar stays
  // in one place rather than being duplicated in JavaScript.
  useEffect(() => {
    if (!value.trim()) { setProblem(null); return }
    const id = setTimeout(() => {
      validate(value)
        .then(r => setProblem(r.valid ? null : r.error))
        .catch(() => setProblem(null))
    }, 250)
    return () => clearTimeout(id)
  }, [value])

  return (
    <>
      <div className="querybar">
        <div className="query-input-wrap">
          <input
            className={`query-input ${problem ? 'invalid' : ''}`}
            value={value}
            spellCheck={false}
            aria-label="Log query"
            placeholder="level:ERROR AND service:billing-api AND response_time > 1000"
            onChange={e => onChange(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !problem) onSubmit() }}
          />
          {problem && <div className="query-error">{problem}</div>}
        </div>
        <button className="run" onClick={onSubmit} disabled={busy || !!problem}>
          {busy ? 'Searching' : 'Search'}
        </button>
      </div>

      <div className="examples">
        {EXAMPLES.map(ex => (
          <button key={ex} className="example" onClick={() => { onChange(ex); }}>
            {ex}
          </button>
        ))}
      </div>
    </>
  )
}
