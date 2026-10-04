import { Play } from 'lucide-react'
import { useEffect, useRef } from 'react'
import styles from './ReplayPage.module.css'

/**
 * Code Pane (UI 디자인 문서 §5.1, 디자인 설계서 §8.2 "읽기 전용 코드, source span 강조").
 *
 * 지금 걸음을 부른 줄을 화살표와 배경으로 짚는다. 줄 번호 중 이벤트를 부른 줄은 버튼이다 — 누르면
 * 그 줄의 다음 걸음으로 간다 (CodeTraceLink 의 코드 → 상태 방향). 캔버스에서 칸을 고르면 그 칸을
 * 건드린 줄이 옅게 칠해진다 (상태 → 코드 방향).
 *
 * Monaco 를 쓰지 않는다. 고칠 수 없는 코드에 편집기 하나(수 MB)를 띄울 이유가 없고, 줄마다 버튼을
 * 다는 것은 Monaco 장식(decoration)보다 평범한 목록이 접근성에 낫다.
 */
export function CodeTracePane({
  source,
  activeLine,
  touchedLines,
  eventLines,
  focusLine,
  onLine,
  onUserScroll,
}: {
  /** 코드. null 이면 볼 수 없는 제출(남의 것)이다. */
  source: string | null
  activeLine: number | null
  touchedLines: Set<number>
  eventLines: Set<number>
  /** "N번 줄 보기"가 부른 줄. 바뀌면 그 줄로 스크롤하고 포커스를 옮긴다. */
  focusLine: { line: number } | null
  onLine: (line: number) => void
  onUserScroll: () => void
}) {
  const container = useRef<HTMLDivElement>(null)

  // 활성 줄이 화면 밖이면 데려온다. 페이지가 아니라 이 pane 만 스크롤한다.
  useEffect(() => {
    if (activeLine === null) return
    reveal(container.current, activeLine, false)
  }, [activeLine])

  useEffect(() => {
    if (focusLine) reveal(container.current, focusLine.line, true)
  }, [focusLine])

  if (source === null) {
    return (
      <p className={styles.paneNote}>
        이 제출의 코드는 공개되지 않았습니다. 상태와 걸음은 그대로 볼 수 있습니다.
      </p>
    )
  }

  const lines = source.replace(/\n$/, '').split('\n')

  return (
    <div
      ref={container}
      className={styles.code}
      onWheel={onUserScroll}
      onTouchMove={onUserScroll}
    >
      <ol className={styles.lines}>
        {lines.map((text, index) => {
          const line = index + 1
          const active = line === activeLine
          return (
            <li
              key={line}
              data-line={line}
              aria-current={active ? 'step' : undefined}
              className={[
                styles.line,
                active ? styles.lineActive : '',
                touchedLines.has(line) ? styles.lineTouched : '',
              ].join(' ')}
            >
              <span className={styles.arrow} aria-hidden="true">
                {active && <Play size={10} fill="currentColor" />}
              </span>
              {eventLines.has(line) ? (
                <button
                  type="button"
                  className={styles.lineNumber}
                  aria-label={`${line}번 줄의 다음 걸음으로`}
                  onClick={() => onLine(line)}
                >
                  {line}
                </button>
              ) : (
                <span className={styles.lineNumber}>{line}</span>
              )}
              <code className={styles.lineText}>{text || ' '}</code>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

function reveal(container: HTMLElement | null, line: number, focus: boolean) {
  const row = container?.querySelector<HTMLElement>(`[data-line="${line}"]`)
  if (!container || !row) return
  const top = row.offsetTop
  if (top < container.scrollTop || top + row.offsetHeight > container.scrollTop + container.clientHeight) {
    container.scrollTop = Math.max(0, top - container.clientHeight / 3)
  }
  if (focus) row.querySelector<HTMLElement>('button')?.focus({ preventScroll: true })
}
