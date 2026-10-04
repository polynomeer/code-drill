import { useQuery } from '@tanstack/react-query'
import { ArrowRight } from 'lucide-react'
import { Link } from 'wouter'
import { listContests } from '../../api/client'
import { Badge, Panel, Skeleton } from '../../design'
import { STATUS_LABEL, countdownOf, remaining, sectionOf } from './contestView'
import { useNow } from './useNow'
import styles from './ContestSummaryPanel.module.css'

/**
 * 홈의 대회 요약 — 진행 중이거나 곧 열릴 것만, 셋까지. 참가·순위표는 `/contests` 에 있다.
 */
export function ContestSummaryPanel() {
  const query = useQuery({ queryKey: ['contests', 'active'], queryFn: () => listContests('active') })
  const contests = (query.data ?? []).filter((contest) => sectionOf(contest) !== 'finished').slice(0, 3)
  const now = useNow(contests.some((contest) => countdownOf(contest) !== null))

  return (
    <Panel
      title="대회"
      actions={
        <Link href="/contests" className={styles.more}>
          전체 <ArrowRight size={14} aria-hidden="true" />
        </Link>
      }
    >
      {query.isError ? (
        <p className={styles.muted}>대회를 불러오지 못했습니다.</p>
      ) : query.isPending ? (
        <Skeleton height={48} />
      ) : contests.length === 0 ? (
        <p className={styles.muted}>진행 중이거나 예정된 대회가 없습니다.</p>
      ) : (
        <ul className={styles.items}>
          {contests.map((contest) => {
            const countdown = countdownOf(contest)
            return (
              <li key={contest.id}>
                <Link href={`/contests/${contest.id}`} className={styles.link}>
                  {contest.title}
                </Link>
                <span className={styles.muted}>
                  <Badge tone={contest.status === 'RUNNING' ? 'success' : 'neutral'}>{STATUS_LABEL[contest.status]}</Badge>{' '}
                  {countdown && `${countdown.label} ${remaining(countdown.target, now)}`}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </Panel>
  )
}
