const time = ts => {
  const d = new Date(ts)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`
}

export default function LogTable({ hits, busy, error }) {
  if (error) {
    return (
      <div className="empty">
        <p>That query could not run.</p>
        <p style={{ color: 'var(--error)' }}>{error}</p>
      </div>
    )
  }

  if (!hits.length) {
    return (
      <div className="empty">
        <p>{busy ? 'Searching the index…' : 'No log lines match this query.'}</p>
        {!busy && <p>Try widening the window, for example <code>timestamp &gt;= now-1h</code>.</p>}
      </div>
    )
  }

  return (
    <div className="table-scroll">
      <table className="logs">
        <thead>
          <tr>
            <th className="col-time">Time</th>
            <th className="col-level">Level</th>
            <th className="col-service">Service</th>
            <th className="col-rt">Response</th>
            <th className="col-msg">Message</th>
          </tr>
        </thead>
        <tbody>
          {hits.map((h, i) => (
            <tr key={`${h.traceId}-${i}`} className={`row-${h.level}`}>
              <td className="col-time">{time(h.timestamp)}</td>
              <td className="col-level"><span className={`badge ${h.level}`}>{h.level}</span></td>
              <td className="col-service" title={h.host}>{h.service}</td>
              <td className={`col-rt ${h.responseTime > 1000 ? 'slow' : ''}`}>
                {h.responseTime ? `${h.responseTime} ms` : '—'}
              </td>
              <td className="col-msg"><span className="msg" title={h.message}>{h.message}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
