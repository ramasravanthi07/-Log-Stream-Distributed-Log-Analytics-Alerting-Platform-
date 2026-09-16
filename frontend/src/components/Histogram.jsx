import ReactECharts from 'echarts-for-react'
import { useMemo } from 'react'

const fmt = ts => new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

export default function Histogram({ buckets, onZoom }) {
  const option = useMemo(() => {
    const data = buckets.map(b => [b.timestamp, b.count])
    const width = buckets.length > 1
      ? buckets[1].timestamp - buckets[0].timestamp
      : 60000

    return {
      grid: { left: 52, right: 16, top: 18, bottom: 26 },
      animation: false,
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#16232f',
        borderColor: '#1e2f3d',
        textStyle: { color: '#c7d5e0', fontFamily: 'IBM Plex Mono' },
        formatter: p => {
          const [ts, count] = p[0].value
          return `${new Date(ts).toLocaleString()}<br/>${count.toLocaleString()} lines`
        }
      },
      xAxis: {
        type: 'time',
        axisLine: { lineStyle: { color: '#1e2f3d' } },
        axisLabel: { color: '#74899c', fontFamily: 'IBM Plex Mono', fontSize: 10, formatter: fmt },
        splitLine: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#74899c', fontFamily: 'IBM Plex Mono', fontSize: 10 },
        splitLine: { lineStyle: { color: '#16222d' } }
      },
      series: [{
        type: 'bar',
        data,
        barCategoryGap: '12%',
        large: true,
        itemStyle: { color: '#3d7f8c' },
        emphasis: { itemStyle: { color: '#4fa8b8' } }
      }],
      _width: width
    }
  }, [buckets])

  const onEvents = {
    click: params => {
      if (!onZoom || !Array.isArray(params.value)) return
      const from = params.value[0]
      onZoom(from, from + option._width)
    }
  }

  if (!buckets.length) {
    return <div style={{ height: 150 }} />
  }

  return (
    <ReactECharts
      option={option}
      onEvents={onEvents}
      style={{ height: 150 }}
      opts={{ renderer: 'canvas' }}
      notMerge
    />
  )
}
