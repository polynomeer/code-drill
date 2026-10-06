import { useQuery } from '@tanstack/react-query'
import { Flame } from 'lucide-react'
import { getPrescription, getStats, getWeeklyReport } from '../../api/client'
import type { Session } from '../../api/session'
import { ProgressBar, Skeleton } from '../../design'
import { attendance } from './questView'
import styles from './Quest.module.css'

/**
 * 플레이어 카드 — 누가, 얼마나 해 왔나. 숫자는 전부 서버가 센 것이다 (푼 문제·제출·연속·주간 활동일).
 * 레벨과 경험치는 그리지 않는다 — 정할 규칙이 아직 없다 (questView.ts).
 */
export function PlayerCard({ session }: { session: Session }) {
  const stats = useQuery({ queryKey: ['me', 'stats'], queryFn: getStats })
  const weekly = useQuery({ queryKey: ['me', 'weekly'], queryFn: getWeeklyReport })
  const prescription = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const streak = prescription.data?.streak
  const week = weekly.data ? attendance(weekly.data.activity.activeDays) : null

  return (
    <section className={styles.card} aria-labelledby="player-name">
      <div className={styles.player}>
        <span className={styles.avatar} aria-hidden="true">
          {session.displayName.slice(0, 1)}
        </span>
        <div>
          <h2 id="player-name" className={styles.playerName}>
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
            <dt>클리어</dt>
            <dd>{stats.data.problemsSolved}</dd>
          </div>
          <div>
            <dt>도전</dt>
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
            이번 주 출석 <strong>{week.days}</strong> / {week.of}일
          </p>
          <ProgressBar label="이번 주 출석" value={week.days} max={week.of} tone="brand" />
        </div>
      )}
    </section>
  )
}
