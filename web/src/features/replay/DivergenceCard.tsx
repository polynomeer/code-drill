import { useQuery } from '@tanstack/react-query'
import { CircleCheck, GitFork, Info } from 'lucide-react'
import { getDivergence } from '../../api/client'
import { Button } from '../../design'
import type { Divergence } from '../../shared/types'
import { describeDivergenceStep } from './timeline'
import styles from './DivergenceCard.module.css'

/**
 * 최초 분기 (PRD FR-805, 디자인 설계서 §2.2, UI 디자인 문서 §5.4).
 *
 * > '틀렸습니다'는 막다른 결과가 아니라, 최초 분기점을 관찰하는 학습 진입점입니다.
 *
 * UI §5.4 의 다섯 단계 순서로 말한다 — 같았던 마지막 상태, 두 풀이의 다음 걸음, 차이를 한 문장,
 * 그 차이가 드러난 케이스, 고칠 방향. **참조 풀이를 보여주지 않는다.** 나오는 것은 갈라진 그 한
 * 걸음뿐이고, 그 앞은 어차피 내 것과 같았으며, 그 뒤는 알려 주면 정답이 된다. 다섯째 단계도 같다 —
 * 고칠 "곳"(내 코드의 줄)은 짚지만 고칠 "답"은 말하지 않는다.
 *
 * 접근이 아예 다르면 지점을 짚지 않는다. "1번에서 갈렸습니다"는 아무것도 알려 주지
 * 못하면서 멀쩡한 첫 줄을 의심하게 만든다.
 */
export function DivergenceCard({
  submissionId,
  onSeek,
  onShowLine,
  caseLabel,
}: {
  submissionId: string
  /** 그 걸음으로 간다. 걸음 N = seq N 을 적용한 상태. */
  onSeek: (step: number) => void
  /** 코드 pane 이 곁에 있으면 그 줄을 짚는다. */
  onShowLine?: (line: number) => void
  /** 이 트레이스를 만든 케이스 — "최소 반례" 자리에 쓴다. */
  caseLabel?: string
}) {
  const divergence = useDivergence(submissionId)

  if (!divergence || divergence.outcome === 'PENDING') return null

  if (divergence.outcome === 'NO_REFERENCE') {
    return (
      <p className={styles.note}>
        <Info size={16} aria-hidden="true" />이 문제는 참조 풀이와 견줄 수 없어 분기를 짚지 못합니다.
      </p>
    )
  }

  if (divergence.outcome === 'SAME') {
    return (
      <p className={`${styles.note} ${styles.same}`}>
        <CircleCheck size={16} aria-hidden="true" />이 케이스에서는 참조 풀이와 같은 길을 갔습니다.
      </p>
    )
  }

  if (divergence.outcome === 'DIFFERENT_APPROACH') {
    return (
      <p className={styles.note}>
        <Info size={16} aria-hidden="true" />
        참조 풀이와 다른 접근입니다. 갈라진 한 지점을 짚을 수 없어, 비교 대신 실행 자체를 되짚어 보세요.
      </p>
    )
  }

  const at = divergence.divergedAtSeq
  const mine = describeDivergenceStep(divergence.actualStep)
  const reference = describeDivergenceStep(divergence.expectedStep)
  const line = divergence.sourceLine

  return (
    <section className={styles.card} aria-labelledby={`divergence-${submissionId}`}>
      <h3 id={`divergence-${submissionId}`} className={styles.title}>
        <GitFork size={16} aria-hidden="true" />
        {at !== null ? `${at}번째 걸음에서 참조 풀이와 갈렸습니다` : '참조 풀이와 갈렸습니다'}
      </h3>
      <ol className={styles.steps}>
        <li>
          <span className={styles.stepLabel}>같았던 마지막 상태</span>
          <span>
            {divergence.sharedPrefix ?? 0}걸음까지 같았습니다.{' '}
            {at !== null && at > 1 && (
              <button type="button" className="linklike" onClick={() => onSeek(at - 1)}>
                그 상태 보기
              </button>
            )}
          </span>
        </li>
        <li>
          <span className={styles.stepLabel}>다음 걸음</span>
          <dl className={styles.compare}>
            <div>
              <dt>내 코드</dt>
              <dd>{mine}</dd>
            </div>
            <div>
              <dt>참조 풀이</dt>
              <dd>{reference}</dd>
            </div>
          </dl>
        </li>
        <li>
          <span className={styles.stepLabel}>차이</span>
          <span>
            같은 상태에서 참조 풀이는 「{reference}」, 내 코드는 「{mine}」
            {line !== null && ` — ${line}번 줄`}.
          </span>
        </li>
        {caseLabel && (
          <li>
            <span className={styles.stepLabel}>드러난 케이스</span>
            <code className={styles.case}>{caseLabel}</code>
          </li>
        )}
        <li>
          <span className={styles.stepLabel}>고칠 방향</span>
          <span>
            {line !== null
              ? `${line}번 줄에서 이 걸음을 고른 조건을 다시 보세요. 해설은 고쳐 본 뒤에 열어도 늦지 않습니다.`
              : '이 걸음을 부른 조건을 다시 보세요. 해설은 고쳐 본 뒤에 열어도 늦지 않습니다.'}
          </span>
        </li>
      </ol>
      <div className={styles.actions}>
        {at !== null && (
          <Button size="dense" variant="primary" onClick={() => onSeek(at)}>
            갈린 걸음으로 이동
          </Button>
        )}
        {line !== null && onShowLine && (
          <Button size="dense" onClick={() => onShowLine(line)}>
            {line}번 줄 보기
          </Button>
        )}
      </div>
    </section>
  )
}

/**
 * 참조 실행이 뒤따라올 수 있다. PENDING 이면 한 번 더 물어본다 — 계속 물으면 열어 둔 화면 하나가
 * 서버를 두드리는 시계가 된다. 타임라인의 오류 마커도 같은 결과를 쓴다 (같은 쿼리 키).
 */
export function useDivergence(submissionId: string): Divergence | null {
  const query = useQuery({
    queryKey: ['submission', submissionId, 'divergence'],
    queryFn: () => getDivergence(submissionId).catch(() => null),
    refetchInterval: (current) => {
      const data = current.state.data
      const pending = data === null || data === undefined || data.outcome === 'PENDING'
      return pending && current.state.dataUpdateCount < 2 ? 4000 : false
    },
  })
  return query.data ?? null
}
