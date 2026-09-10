import { useEffect, useState } from 'react'
import { getDivergence } from '../../api/client'
import type { Divergence } from '../../shared/types'

/**
 * 최초 분기 (PRD FR-805, 디자인 설계서 §2.2).
 *
 * > '틀렸습니다'는 막다른 결과가 아니라, 최초 분기점을 관찰하는 학습 진입점입니다.
 *
 * **참조 풀이를 보여주지 않는다.** 여기 나오는 것은 갈라진 그 한 걸음뿐이고, 그 앞은
 * 어차피 내 것과 같았으며, 그 뒤는 알려 주면 정답이 된다.
 *
 * 접근이 아예 다르면 지점을 짚지 않는다. "1번에서 갈렸습니다"는 아무것도 알려 주지
 * 못하면서 멀쩡한 첫 줄을 의심하게 만든다.
 */
export function DivergenceCard({
  submissionId,
  onSeek,
}: {
  submissionId: string
  onSeek: (seq: number) => void
}) {
  const [divergence, setDivergence] = useState<Divergence | null>(null)

  useEffect(() => {
    setDivergence(null)
    let cancelled = false
    // 참조 실행이 뒤따라올 수 있다. PENDING 이면 한 번 더 물어본다 — 계속 물으면
    // 열어 둔 화면 하나가 서버를 두드리는 시계가 된다.
    const load = async (retry: boolean) => {
      const next = await getDivergence(submissionId).catch(() => null)
      if (cancelled) return
      setDivergence(next)
      if (retry && (next === null || next.outcome === 'PENDING')) {
        window.setTimeout(() => void load(false), 4000)
      }
    }
    void load(true)
    return () => {
      cancelled = true
    }
  }, [submissionId])

  if (!divergence || divergence.outcome === 'PENDING') return null

  if (divergence.outcome === 'NO_REFERENCE') {
    return (
      <p className="muted small">
        이 문제는 참조 풀이와 견줄 수 없어 분기를 짚지 못합니다.
      </p>
    )
  }

  if (divergence.outcome === 'SAME') {
    return (
      <p className="ok small">
        ✓ 이 케이스에서는 참조 풀이와 같은 길을 갔습니다.
      </p>
    )
  }

  if (divergence.outcome === 'DIFFERENT_APPROACH') {
    return (
      <p className="muted small">
        참조 풀이와 다른 접근입니다. 갈라진 한 지점을 짚을 수 없어, 비교 대신 실행 자체를
        되짚어 보세요.
      </p>
    )
  }

  return (
    <div className="divergence">
      <p className="warn small">
        {divergence.sharedPrefix}걸음까지는 참조 풀이와 같았고, 그 다음에서 갈렸습니다.
      </p>
      <ul className="divergence-steps">
        <li>
          <span className="muted small">내 풀이</span>
          <span className="mono small">{divergence.actualStep}</span>
        </li>
        <li>
          <span className="muted small">참조 풀이</span>
          <span className="mono small">{divergence.expectedStep}</span>
        </li>
      </ul>
      <p className="muted small">
        {divergence.sourceLine !== null && <>내 코드 {divergence.sourceLine}번째 줄 · </>}
        {divergence.divergedAtSeq !== null && (
          <button
            type="button"
            className="linklike"
            onClick={() => onSeek(divergence.divergedAtSeq as number)}
          >
            그 지점으로 이동
          </button>
        )}
      </p>
    </div>
  )
}
