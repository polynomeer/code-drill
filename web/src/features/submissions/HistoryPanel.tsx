import { VERDICT_LABEL } from '../../shared/types'
import type { Submission } from '../../shared/types'

/**
 * 제출 기록 (기술 설계서 §16.1 P3 기록, 디자인 설계서 §2.1 제출).
 *
 * 판정과 점수, 언어, 시각을 한 줄로 보여주고 클릭하면 그 제출을 다시 연다. 정렬은
 * 서버가 고정한 최신순 그대로다 — 클라이언트에서 다시 정렬하면 커서로 이어본 다음
 * 페이지와 순서가 어긋난다.
 */
export function HistoryPanel({
  submissions,
  currentId,
  onOpen,
}: {
  submissions: Submission[]
  currentId: string | null
  onOpen: (id: string) => void
}) {
  if (submissions.length === 0) {
    return (
      <section className="panel">
        <h3>제출 기록</h3>
        <p className="muted">아직 제출이 없습니다.</p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h3>제출 기록</h3>
      <ul className="history">
        {submissions.map((item) => (
          <li key={item.id} className={item.id === currentId ? 'current' : ''}>
            <button onClick={() => onOpen(item.id)}>
              <span className={verdictClass(item)}>
                {item.verdict ? VERDICT_LABEL[item.verdict] : item.status}
              </span>
              <span className="muted">{item.language}</span>
              <span className="muted">{item.score !== null ? `${item.score}점` : '—'}</span>
              <span className="muted mono">{item.id.slice(0, 8)}</span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** 색만으로 구분하지 않는다. 텍스트가 이미 판정을 말하고, 색은 거들기만 한다. */
function verdictClass(submission: Submission): string {
  if (submission.verdict === 'ACCEPTED') return 'ok'
  return submission.verdict ? 'bad' : 'muted'
}
