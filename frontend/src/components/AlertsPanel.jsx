import { useEffect, useState } from 'react'
import * as api from '../api'

const blank = {
  name: '',
  query: 'level:ERROR AND service:billing-api',
  threshold: 100,
  windowMinutes: 5,
  webhookUrl: ''
}

export default function AlertsPanel({ onInspect }) {
  const [data, setData] = useState({ rules: [], firing: [], events: [] })
  const [draft, setDraft] = useState(blank)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState(null)

  const load = () => api.listAlerts().then(setData).catch(e => setNotice(e.message))

  useEffect(() => {
    load()
    const id = setInterval(load, 10000)
    return () => clearInterval(id)
  }, [])

  const submit = async () => {
    if (!draft.name.trim() || !draft.query.trim()) {
      setNotice('A rule needs a name and a query.')
      return
    }
    setSaving(true)
    try {
      await api.createAlert({
        ...draft,
        threshold: Number(draft.threshold),
        windowMinutes: Number(draft.windowMinutes)
      })
      setDraft(blank)
      setNotice(null)
      load()
    } catch (e) {
      setNotice(e.message)
    } finally {
      setSaving(false)
    }
  }

  const firing = new Set(data.firing ?? [])

  return (
    <div className="alerts">
      <div>
        <h2>Rules</h2>
        {data.rules.length === 0 && <p style={{ color: 'var(--text-dim)' }}>No rules yet. Create one on the right.</p>}

        {data.rules.map(r => (
          <article key={r.id} className={`rule ${firing.has(r.id) ? 'firing' : ''}`}>
            <div className="rule-head">
              <h3>{r.name}</h3>
              <span className={`state ${firing.has(r.id) ? 'firing' : 'ok'}`}>
                {firing.has(r.id) ? 'firing' : 'normal'}
              </span>
            </div>
            <div className="rule-query">{r.query}</div>
            <div className="rule-meta">
              <span>fires above {r.threshold} in {r.windowMinutes}m</span>
              <span>last count {r.lastCount ?? 0}</span>
              <span>fired {r.timesFired ?? 0}x</span>
              {r.webhookUrl && <span>webhook set</span>}
            </div>
            <div className="rule-actions">
              <button onClick={() => api.evaluateAlert(r.id).then(load)}>Check now</button>
              <button onClick={() => onInspect(`(${r.query}) AND timestamp >= now-${r.windowMinutes}m`)}>
                View matching logs
              </button>
              <button onClick={() => api.deleteAlert(r.id).then(load)}>Delete</button>
            </div>
          </article>
        ))}
      </div>

      <div>
        <h2>New rule</h2>
        <div className="form">
          <label>
            Name
            <input value={draft.name} placeholder="Checkout 500s"
                   onChange={e => setDraft({ ...draft, name: e.target.value })} />
          </label>
          <label>
            Query
            <input value={draft.query} spellCheck={false}
                   onChange={e => setDraft({ ...draft, query: e.target.value })} />
          </label>
          <div className="two-up">
            <label>
              Fires above
              <input type="number" min="1" value={draft.threshold}
                     onChange={e => setDraft({ ...draft, threshold: e.target.value })} />
            </label>
            <label>
              Window (minutes)
              <input type="number" min="1" value={draft.windowMinutes}
                     onChange={e => setDraft({ ...draft, windowMinutes: e.target.value })} />
            </label>
          </div>
          <label>
            Webhook URL (optional)
            <input value={draft.webhookUrl} placeholder="https://hooks.slack.com/services/…"
                   onChange={e => setDraft({ ...draft, webhookUrl: e.target.value })} />
          </label>
          {notice && <div className="query-error">{notice}</div>}
          <button className="submit" onClick={submit} disabled={saving}>
            {saving ? 'Saving' : 'Create rule'}
          </button>
        </div>

        <h2 style={{ marginTop: 28 }}>Recent activity</h2>
        {(data.events ?? []).length === 0 && (
          <p style={{ color: 'var(--text-dim)' }}>Nothing has fired yet. Rules are checked every minute.</p>
        )}
        {(data.events ?? []).slice(0, 20).map((e, i) => (
          <div className="event" key={i}>
            <time>{new Date(e.timestamp).toLocaleTimeString()}</time>
            <span className={e.state === 'FIRING' ? 'firing' : 'resolved'}>
              {e.state === 'FIRING' ? 'fired' : 'resolved'}
            </span>
            <span>{e.ruleName} — {e.count} vs {e.threshold}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
