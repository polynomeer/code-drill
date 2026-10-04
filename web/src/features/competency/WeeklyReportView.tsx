import { useQuery } from '@tanstack/react-query'
import { Link } from 'wouter'
import { getStats, getWeeklyReport } from '../../api/client'
import { Badge, InlineAlert, Skeleton } from '../../design'
import { COMPETENCY_LABEL, LEVEL_LABEL, REASON_LABEL, VERDICT_LABEL } from '../../shared/types'
import type { Verdict } from '../../shared/types'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { ENOUGH_SUBMISSIONS, enoughData } from './competencyView'
import styles from './CompetenciesPage.module.css'

/**
 * 이번 주 (PRD FR-808, UI 디자인 문서 §9.4 Weekly growth — "성장, 재발 오류, 전이 성과, 다음 측정").
 *
 * 약점·재발·성장 세 칸이 요구사항의 세 단어 그대로다. "다음 행동"은 오늘의 처방과 같은 것을 보인다 —
 * 리포트가 행동을 따로 지어내면 처방과 리포트가 서로 다른 말을 한다.
 *
 * 비어 있는 칸도 문장으로 말한다. "지난주보다 오른 것이 없다"도 사실이다.
 */
export function WeeklyReportView({ onSelectCompetency }: { onSelectCompetency: (competency: string) => void }) {
  const report = useQuery({ queryKey: ['me', 'weekly'], queryFn: getWeeklyReport })
  const stats = useQuery({ queryKey: ['me', 'stats'], queryFn: getStats })
  const index = useProblemIndex()
  const date = (iso: string) => new Date(iso).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })

  if (report.isError) return <InlineAlert tone="danger" title="이번 주 리포트를 불러오지 못했습니다" />
  if (!report.data) {
    return (
      <div role="status" aria-busy="true" className={styles.loading}>
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton height={80} />
        <Skeleton height={200} />
      </div>
    )
  }

  const data = report.data
  const { activity } = data

  return (
    <div className={styles.weekly}>
      <p className={styles.muted}>
        {data.from} ~ {data.to}
      </p>
      <dl className={styles.tiles}>
        <div>
          <dt>시도</dt>
          <dd>{activity.attempts}회</dd>
        </div>
        <div>
          <dt>맞힘</dt>
          <dd>{activity.accepted}회</dd>
        </div>
        <div>
          <dt>푼 문제</dt>
          <dd>{activity.problemsSolved}개</dd>
        </div>
        <div>
          <dt>활동한 날</dt>
          <dd>{activity.activeDays}일</dd>
        </div>
      </dl>

      <section aria-labelledby="weekly-weak" className={styles.weeklySection}>
        <h2 id="weekly-weak" className={styles.groupTitle}>
          흔들리는 역량
        </h2>
        {data.weakest.length === 0 ? (
          <p className={styles.muted}>지금 흔들리는 역량이 없습니다.</p>
        ) : (
          <ul className={styles.chips}>
            {data.weakest.map((competency) => (
              <li key={competency}>
                <button type="button" className={styles.chipButton} onClick={() => onSelectCompetency(competency)}>
                  {COMPETENCY_LABEL[competency] ?? competency} — 근거 보기
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="weekly-recur" className={styles.weeklySection}>
        <h2 id="weekly-recur" className={styles.groupTitle}>
          재발
        </h2>
        {data.recurrences.length === 0 ? (
          <p className={styles.muted}>전에 맞힌 문제를 다시 틀린 일은 없습니다.</p>
        ) : (
          <ul className={styles.plainList}>
            {data.recurrences.map((r) => (
              <li key={r.problemId}>
                <Link href={`/problems/${r.problemId}/solve`}>{problemLabel(index, r.problemId)}</Link>
                <span className={styles.muted}>
                  {' '}
                  — {date(r.solvedAt)}에 맞혔는데 {date(r.failedAt)}에 다시 틀렸습니다
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="weekly-growth" className={styles.weeklySection}>
        <h2 id="weekly-growth" className={styles.groupTitle}>
          성장
        </h2>
        {data.growth.length === 0 ? (
          <p className={styles.muted}>지난주보다 오른 역량이 아직 없습니다.</p>
        ) : (
          <ul className={styles.plainList}>
            {data.growth.map((g) => (
              <li key={g.competency}>
                <button type="button" className="linklike" onClick={() => onSelectCompetency(g.competency)}>
                  {COMPETENCY_LABEL[g.competency] ?? g.competency}
                </button>
                : {LEVEL_LABEL[g.from]} → <strong>{LEVEL_LABEL[g.to]}</strong>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="weekly-next" className={styles.weeklySection}>
        <h2 id="weekly-next" className={styles.groupTitle}>
          다음 행동
        </h2>
        {data.actions.length === 0 ? (
          <p className={styles.muted}>권할 것이 없습니다.</p>
        ) : (
          <ul className={styles.plainList}>
            {data.actions.map((a) => (
              <li key={a.problemId}>
                <Badge>{REASON_LABEL[a.reason]}</Badge>{' '}
                <Link href={`/problems/${a.problemId}/solve`}>{problemLabel(index, a.problemId)}</Link>
              </li>
            ))}
          </ul>
        )}
        <p className={styles.muted}>다음 측정: {date(data.nextMeasurement)}</p>
      </section>

      {stats.data && (
        <section aria-labelledby="weekly-stats" className={styles.weeklySection}>
          <h2 id="weekly-stats" className={styles.groupTitle}>
            통계
          </h2>
          {/* 시도가 적으면 정답률을 말하지 않는다 (DS §10.4) — 한 번의 실수가 20% 가 된다 */}
          {!enoughData(stats.data.submissions) ? (
            <p className={styles.muted}>
              데이터가 더 필요합니다 — 제출 {ENOUGH_SUBMISSIONS}회부터 정답률과 주제별 기록을 보입니다. 지금 {stats.data.submissions}회.
            </p>
          ) : (
            <>
              <p>
                시도한 문제 {stats.data.problemsAttempted}개 중 {stats.data.problemsSolved}개 해결 · 제출{' '}
                {stats.data.submissions}회 중 {stats.data.accepted}회 정답
              </p>
              {Object.keys(stats.data.verdicts).length > 0 && (
                <p className={styles.muted}>
                  최근 30일:{' '}
                  {Object.entries(stats.data.verdicts)
                    .map(([v, n]) => `${VERDICT_LABEL[v as Verdict] ?? v} ${n}`)
                    .join(' · ')}
                </p>
              )}
              {Object.keys(stats.data.byTag).length > 0 && (
                <ul className={styles.tagStats} aria-label="주제별">
                  {Object.entries(stats.data.byTag).map(([tag, t]) => (
                    <li key={tag}>
                      <span>{tag}</span>
                      <span className={styles.count}>
                        {t.solved}/{t.attempted}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </section>
      )}
    </div>
  )
}
