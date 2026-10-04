import { activitySummary, levelOf, monthLabels, weeks } from './profileView'
import type { Day } from './profileView'
import styles from './ProfilePage.module.css'

const CELL = 11
const GAP = 3
const STEP = CELL + GAP
const LEFT = 22
const TOP = 16
const WEEKDAYS: [number, string][] = [
  [1, '월'],
  [3, '수'],
  [5, '금'],
]

/**
 * 1년 활동 히트맵 (docs/ui-overhaul.md §6.7).
 *
 * 진하기는 고정 문턱이다 (profileView.levelOf). 칸마다 날짜와 제출 수가 `<title>` 로 붙고, 그림 전체는
 * 한 문장 요약으로 읽힌다 — 365칸을 하나씩 읽게 하지 않는다. 좁은 화면에서는 가로로 스크롤한다.
 */
export function ActivityHeatmap({ days }: { days: Day[] }) {
  const columns = weeks(days)
  const months = monthLabels(columns)
  const summary = activitySummary(days)
  // 마지막 달 이름이 오른쪽 끝 열에서 시작해도 잘리지 않게 두 칸을 더 둔다
  const width = LEFT + columns.length * STEP + 2 * STEP
  const height = TOP + 7 * STEP

  return (
    <div className={styles.heatmap}>
      <p className={styles.muted}>
        최근 1년 제출 {summary.submissions}회 · 활동한 날 {summary.activeDays}일
      </p>
      <div className={styles.heatmapScroll} tabIndex={0} role="region" aria-label="활동 히트맵 — 가로로 스크롤">
        <svg width={width} height={height} role="img" aria-label={`최근 1년 제출 ${summary.submissions}회, 활동한 날 ${summary.activeDays}일`}>
          {months.map(
            (label, column) =>
              label && (
                <text key={column} x={LEFT + column * STEP} y={10} className={styles.heatmapLabel}>
                  {label}
                </text>
              ),
          )}
          {WEEKDAYS.map(([row, label]) => (
            <text key={label} x={0} y={TOP + row * STEP + CELL - 2} className={styles.heatmapLabel}>
              {label}
            </text>
          ))}
          {columns.map((column, x) =>
            column.map(
              (day, y) =>
                day && (
                  <rect
                    key={day.date}
                    x={LEFT + x * STEP}
                    y={TOP + y * STEP}
                    width={CELL}
                    height={CELL}
                    rx={2}
                    className={styles[`level${levelOf(day.submissions)}`]}
                  >
                    <title>{`${day.date} — 제출 ${day.submissions}회`}</title>
                  </rect>
                ),
            ),
          )}
        </svg>
      </div>
      <p className={styles.legend} aria-hidden="true">
        적음
        {([0, 1, 2, 3, 4] as const).map((level) => (
          <svg key={level} width={CELL} height={CELL}>
            <rect width={CELL} height={CELL} rx={2} className={styles[`level${level}`]} />
          </svg>
        ))}
        많음
      </p>
    </div>
  )
}
