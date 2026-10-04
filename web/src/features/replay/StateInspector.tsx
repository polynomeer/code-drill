import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { Badge, IconButton, InlineAlert } from '../../design'
import { EVENT_LABEL, describe, describeStep, lastTouch, tierOf } from './timeline'
import type { TraceEvent } from './traceTypes'
import styles from './ReplayPage.module.css'

/**
 * State Inspector (UI 디자인 문서 §5.1 "현재 이벤트, 변수 변화, 학습 포인트 — 변경값 우선 표시").
 *
 * 맨 위가 지금 걸음의 문장이다. 그 아래 before → after, 부른 줄, 그리고 캔버스에서 고른 칸이 있으면
 * "그 칸을 마지막으로 건드린 걸음". 최근 걸음 목록은 텍스트 폴백(디자인 설계서 §8.5)이기도 하다 —
 * 캔버스를 못 보는 사람도 여기서 무엇이 일어났는지 읽고 걸음을 옮긴다.
 */
export function StateInspector({
  current,
  events,
  step,
  divergedAtSeq,
  selected,
  onClearSelection,
  onSeek,
  onShowLine,
  warnings,
  children,
}: {
  current: TraceEvent | null
  events: TraceEvent[]
  step: number
  divergedAtSeq: number | null
  selected: string | null
  onClearSelection: () => void
  onSeek: (step: number) => void
  onShowLine: (line: number) => void
  warnings: string[]
  /** 분기 카드·예측처럼 내 제출일 때만 붙는 것 */
  children?: ReactNode
}) {
  const recent = events.slice(Math.max(0, Math.min(step, events.length) - 6), Math.min(step, events.length)).reverse()
  const touched = selected ? lastTouch(events, step, selected) : null
  const tier = current ? tierOf(current, divergedAtSeq) : null

  return (
    <div className={styles.inspector}>
      <section aria-labelledby="inspector-current">
        <h2 id="inspector-current" className={styles.paneTitle}>
          현재 이벤트
        </h2>
        {current ? (
          <div className={styles.currentEvent}>
            <p className={styles.sentence}>{describeStep(current)}</p>
            <div className={styles.eventMeta}>
              <Badge tone={tier === 'error' ? 'danger' : tier === 'important' ? 'trace' : 'neutral'}>
                {EVENT_LABEL[current.eventType] ?? current.eventType}
              </Badge>
              {tier === 'error' && <Badge tone="danger">분기</Badge>}
              {current.sourceLine !== null && (
                <button type="button" className="linklike" onClick={() => onShowLine(current.sourceLine as number)}>
                  {current.sourceLine}번 줄
                </button>
              )}
            </div>
            {(current.before !== null || current.after !== null) && (
              <dl className={styles.change}>
                <dt>변화</dt>
                <dd>
                  <code>{current.before ?? '—'}</code>
                  <span aria-hidden="true"> → </span>
                  <span className="visually-hidden">에서</span>
                  <code className={styles.after}>{current.after ?? '—'}</code>
                </dd>
              </dl>
            )}
          </div>
        ) : (
          <p className={styles.paneNote}>재생 전 — 처음 상태입니다. →를 누르거나 재생하세요.</p>
        )}
      </section>

      {selected && (
        <section aria-labelledby="inspector-selection" className={styles.selection}>
          <div className={styles.selectionHead}>
            <h2 id="inspector-selection" className={styles.paneTitle}>
              고른 {selected.startsWith('GRAPH:') ? '정점' : '칸'} {selected.split(':')[1]}
            </h2>
            <IconButton label="선택 해제" size="dense" icon={<X size={14} />} onClick={onClearSelection} />
          </div>
          {touched ? (
            <p className={styles.paneNote}>
              마지막으로 건드린 걸음 —{' '}
              <button type="button" className="linklike" onClick={() => onSeek(touched.seq)}>
                단계 {touched.seq}
              </button>
              : {describe(touched)}
            </p>
          ) : (
            <p className={styles.paneNote}>지금 걸음까지 건드린 적이 없습니다.</p>
          )}
        </section>
      )}

      {children}

      {warnings.map((warning) => (
        <InlineAlert key={warning} tone="warning">
          {warning}
        </InlineAlert>
      ))}

      {recent.length > 0 && (
        <section aria-labelledby="inspector-recent">
          <h2 id="inspector-recent" className={styles.paneTitle}>
            최근 걸음
          </h2>
          <ol className={styles.recent}>
            {recent.map((event) => (
              <li key={event.seq}>
                <button
                  type="button"
                  className={styles.recentItem}
                  aria-current={event.seq === step ? 'step' : undefined}
                  onClick={() => onSeek(event.seq)}
                >
                  <span className={styles.recentSeq}>{event.seq}</span>
                  {describe(event)}
                </button>
              </li>
            ))}
          </ol>
        </section>
      )}
    </div>
  )
}
