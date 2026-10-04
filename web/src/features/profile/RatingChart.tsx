import type { PublicProfile } from '../../shared/types'
import { ratingSeries } from './profileView'
import styles from './ProfilePage.module.css'

const WIDTH = 960
const HEIGHT = 200
const PAD = { left: 44, right: 12, top: 12, bottom: 24 }

/**
 * 레이팅 선 그래프 (docs/ui-overhaul.md §6.6 "프로필에 레이팅 선 그래프와 대회 이력").
 *
 * 가로축은 대회 순서다 — 날짜 간격으로 그리면 몇 달 쉰 구간이 선을 길게 늘여 변화가 묻힌다. 같은 값은
 * 아래 이력 표가 글로 준다; 그림은 흐름만 말한다.
 */
export function RatingChart({ history }: { history: NonNullable<PublicProfile['rating']>['history'] }) {
  const series = ratingSeries(history)
  if (series.length < 2) return null

  const values = series.map((point) => point.value)
  const low = Math.floor((Math.min(...values) - 20) / 50) * 50
  const high = Math.ceil((Math.max(...values) + 20) / 50) * 50
  const x = (index: number) => PAD.left + (index / (series.length - 1)) * (WIDTH - PAD.left - PAD.right)
  const y = (value: number) => PAD.top + ((high - value) / (high - low)) * (HEIGHT - PAD.top - PAD.bottom)
  const first = series[0]!
  const last = series[series.length - 1]!

  return (
    <svg
      className={styles.chart}
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      role="img"
      aria-label={`레이팅 ${first.value}에서 ${last.value}, 대회 ${series.length - 1}회`}
    >
      {[low, (low + high) / 2, high].map((tick) => (
        <g key={tick}>
          <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y(tick)} y2={y(tick)} className={styles.chartGrid} />
          <text x={PAD.left - 6} y={y(tick) + 4} textAnchor="end" className={styles.chartLabel}>
            {tick}
          </text>
        </g>
      ))}
      <polyline points={series.map((point, index) => `${x(index)},${y(point.value)}`).join(' ')} className={styles.chartLine} />
      {series.map((point, index) => (
        <circle key={`${point.at}-${index}`} cx={x(index)} cy={y(point.value)} r={index === 0 ? 3 : 4} className={styles.chartPoint}>
          <title>{point.title ? `${point.title} — ${point.value}` : `시작 — ${point.value}`}</title>
        </circle>
      ))}
    </svg>
  )
}
