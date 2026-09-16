const LEVEL_ORDER = ['FATAL', 'ERROR', 'WARN', 'INFO', 'DEBUG']

export default function FacetRail({ result, onPick }) {
  const levels = result?.levelCounts ?? {}
  const services = result?.serviceCounts ?? {}

  const sortedLevels = Object.entries(levels)
    .sort((a, b) => LEVEL_ORDER.indexOf(a[0]) - LEVEL_ORDER.indexOf(b[0]))

  return (
    <aside className="rail">
      <section>
        <h2>Level</h2>
        {sortedLevels.length === 0 && <p className="facet-count" style={{ padding: '0 16px' }}>No data</p>}
        {sortedLevels.map(([name, count]) => (
          <button key={name} className="facet" onClick={() => onPick('level', name)}>
            <span className={`badge ${name}`}>{name}</span>
            <span className="facet-count">{count.toLocaleString()}</span>
          </button>
        ))}
      </section>

      <section>
        <h2>Service</h2>
        {Object.entries(services).map(([name, count]) => (
          <button key={name} className="facet" onClick={() => onPick('service', name)}>
            <span className="facet-name">{name}</span>
            <span className="facet-count">{count.toLocaleString()}</span>
          </button>
        ))}
      </section>
    </aside>
  )
}
