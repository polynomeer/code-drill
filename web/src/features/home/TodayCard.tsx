import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Play } from 'lucide-react'
import { Link } from 'wouter'
import { getPrescription } from '../../api/client'
import { Badge, Skeleton } from '../../design'
import { COMPETENCY_LABEL, REASON_LABEL } from '../../shared/types'
import { markOpenSource } from '../../shared/analytics'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { splitToday } from './homeView'
import styles from './Home.module.css'

/**
 * 오늘의 훈련 — 처방의 첫 문제를 크게, 나머지를 아래에 (docs/ui-overhaul.md §6.10).
 *
 * 교체·미루기는 여기 두지 않는다. 행동은 `/training` 한 곳에만 있다 — 두 곳에 두면 한쪽에서 바꾼 것이
 * 다른 쪽에 늦게 비친다 (TodaySummary 와 같은 이유).
 */
export function TodayCard() {
  const query = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const index = useProblemIndex()
  const today = query.data ? splitToday(query.data.items) : null

  return (
    <section className={styles.card} aria-labelledby="today-title">
      <div className={styles.cardHead}>
        <h2 id="today-title" className={styles.cardTitle}>
          오늘의 훈련
        </h2>
        <Link href="/training" className={styles.more}>
          바꾸기·미루기 <ArrowRight size={14} aria-hidden="true" />
        </Link>
      </div>

      {query.isError ? (
        <p className={styles.muted}>처방을 불러오지 못했습니다.</p>
      ) : !today ? (
        <Skeleton height={120} />
      ) : !today.first ? (
        <p className={styles.muted}>오늘 권할 문제가 없습니다. 아무 문제나 풀어도 기록이 쌓입니다.</p>
      ) : (
        <>
          <div className={styles.focus}>
            <p className={styles.reason}>{REASON_LABEL[today.first.reason]}</p>
            <h3 className={styles.focusTitle}>{problemLabel(index, today.first.problemId)}</h3>
            {today.first.detail && <p className={styles.focusDetail}>{today.first.detail}</p>}
            {today.first.competency && <Badge tone="brand">{COMPETENCY_LABEL[today.first.competency] ?? today.first.competency}</Badge>}
            <Link
              href={`/problems/${today.first.problemId}/solve`}
              className={styles.start}
              onClick={() => markOpenSource('prescription')}
            >
              <Play size={16} aria-hidden="true" />
              시작
            </Link>
          </div>

          {today.rest.length > 0 && (
            <ul className={styles.rest} aria-label="다른 처방">
              {today.rest.map((item) => (
                <li key={item.problemId}>
                  <span className={styles.reason}>{REASON_LABEL[item.reason]}</span>
                  <Link href={`/problems/${item.problemId}/solve`} className={styles.restLink} onClick={() => markOpenSource('prescription')}>
                    {problemLabel(index, item.problemId)}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
