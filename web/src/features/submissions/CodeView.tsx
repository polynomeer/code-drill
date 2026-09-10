import { useEffect, useState } from 'react'
import { getSubmissionSource } from '../../api/client'
import { collapse, diffLines } from './diff'
import type { Submission } from '../../shared/types'
import { VERDICT_LABEL } from '../../shared/types'

/**
 * 제출한 코드와 두 제출의 차이 (PRD §6.4).
 *
 * 코드는 펼칠 때 받아 온다. 목록과 함께 받으면 대부분 읽히지 않을 코드가 오가고, 소스는
 * 그중 가장 민감한 값이다 (§11.1).
 *
 * 비교 대상은 **바로 이전 제출**이 기본이다. 사용자가 두 건을 고르는 UI 를 만들 수도
 * 있지만, 실제로 묻는 질문은 거의 항상 "직전에서 무엇을 고쳤나"다.
 */
export function CodeView({
  submission,
  previous,
  onClose,
}: {
  submission: Submission
  previous: Submission | null
  onClose: () => void
}) {
  // 세 상태를 가른다: undefined = 받는 중, null = 지워져 비었다, 문자열 = 코드.
  // 둘을 null 하나로 뭉개면 계정을 지운 제출이 영원히 "불러오는 중"으로 보인다.
  const [source, setSource] = useState<string | null | undefined>(undefined)
  const [base, setBase] = useState<string | null>(null)
  const [comparing, setComparing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setSource(undefined)
    setBase(null)
    setComparing(false)
    setError(null)
    getSubmissionSource(submission.id)
      .then(setSource)
      .catch((e: Error) => setError(e.message))
  }, [submission.id])

  const compare = async () => {
    if (!previous) return
    try {
      setBase(await getSubmissionSource(previous.id))
      setComparing(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : '이전 제출을 읽지 못했습니다')
    }
  }

  return (
    <section className="panel code-view">
      <div className="editor-header">
        <h3>
          제출 코드 <span className="muted mono small">{submission.id.slice(0, 8)}</span>
        </h3>
        <div className="editor-actions">
          {previous && !comparing && (
            <button type="button" onClick={() => void compare()}>
              이전 제출과 비교
            </button>
          )}
          {comparing && (
            <button type="button" onClick={() => setComparing(false)}>
              코드만 보기
            </button>
          )}
          <button type="button" className="linklike" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>

      {error && <p className="warn">{error}</p>}
      {source === undefined && !error && <p className="muted">코드를 불러오는 중…</p>}
      {/* 지운 계정의 소스는 비어 있다. 빈 화면 대신 왜 없는지 말한다 (§11.3). */}
      {source === null && <p className="muted">이 제출의 코드는 지워졌습니다.</p>}

      {comparing && base !== null && previous && source ? (
        <>
          <p className="muted small">
            {VERDICT_LABEL[previous.verdict ?? 'SYSTEM_ERROR']} → {' '}
            {VERDICT_LABEL[submission.verdict ?? 'SYSTEM_ERROR']}
            {previous.score !== null && submission.score !== null && (
              <> · {previous.score}점 → {submission.score}점</>
            )}
          </p>
          <pre className="code mono diff">
            {collapse(diffLines(base, source)).map((entry, index) =>
              entry.kind === 'gap' ? (
                <span key={index} className="diff-gap">{`  ⋯ ${entry.count}줄\n`}</span>
              ) : (
                <span key={index} className={`diff-${entry.kind}`}>
                  {/* 색만으로 구분하지 않는다. 기호가 먼저 읽힌다. */}
                  {entry.kind === 'added' ? '+ ' : entry.kind === 'removed' ? '- ' : '  '}
                  {entry.text}
                  {'\n'}
                </span>
              ),
            )}
          </pre>
        </>
      ) : (
        source && <pre className="code mono">{source}</pre>
      )}
    </section>
  )
}
