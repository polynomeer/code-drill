import { useQuery } from '@tanstack/react-query'
import { Flame } from 'lucide-react'
import { getPrescription, getStats, getWeeklyReport } from '../../api/client'
import type { Session } from '../../api/session'
import { ProgressBar, Skeleton } from '../../design'
import { activeWeek } from './homeView'
import styles from './Home.module.css'

/**
 * 내 기록 — 누가, 얼마나 해 왔나. 숫자는 전부 서버가 센 것이다 (푼 문제·시도·제출·연속·주간 활동일).
 */
export function RecordCard({ session }: { session: Session }) {
  const stats = useQuery({ queryKey: ['me', 'stats'], queryFn: getStats })
  const weekly = useQuery({ queryKey: ['me', 'weekly'], queryFn: getWeeklyReport })
  const prescription = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const streak = prescription.data?.streak
  const week = weekly.data ? activeWeek(weekly.data.activity.activeDays) : null

  return (
    <section className={styles.card} aria-labelledby="record-name">
      <div className={styles.person}>
        <span className={styles.avatar} aria-hidden="true">
          {session.displayName.slice(0, 1)}
        </span>
        <div>
          <h2 id="record-name" className={styles.personName}>
            {session.displayName}
          </h2>
          {streak && (
            <p className={streak.atRisk && !streak.activeToday ? `${styles.streak} ${styles.atRisk}` : styles.streak}>
              <Flame size={14} aria-hidden="true" />
              {streak.days > 0 ? `${streak.days}일 연속` : '연속 기록 시작 전'}
              {streak.atRisk && !streak.activeToday && ' · 오늘 안 하면 끊깁니다'}
            </p>
          )}
        </div>
      </div>

      {stats.isPending ? (
        <Skeleton height={64} />
      ) : stats.data ? (
        <dl className={styles.tiles}>
          <div>
            <dt>푼 문제</dt>
            <dd>{stats.data.problemsSolved}</dd>
          </div>
          <div>
            <dt>시도</dt>
            <dd>{stats.data.problemsAttempted}</dd>
          </div>
          <div>
            <dt>제출</dt>
            <dd>{stats.data.submissions}</dd>
          </div>
        </dl>
      ) : null}

      {week && (
        <div className={styles.week}>
          <p className={styles.weekLabel}>
            이번 주 활동 <strong>{week.days}</strong> / {week.of}일
          </p>
          <ProgressBar label="이번 주 활동일" value={week.days} max={week.of} tone="brand" />
        </div>
      )}
    </section>
  )
}
