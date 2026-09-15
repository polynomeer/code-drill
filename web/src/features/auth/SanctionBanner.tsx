import { useEffect, useState } from 'react'
import { appealSanction, getMySanction } from '../../api/client'
import type { SanctionView } from '../../shared/types'

const KIND_LABEL = { WARNING: '경고', MUTE: '글쓰기 정지', SUSPEND: '제출·실행 정지' }

/**
 * 내 계정의 제재 (기획서 §8.5 단계적 제재, §10.4 이의 절차).
 *
 * 화면 맨 위에 둔다 — 무엇이 막혔고 언제 풀리는지는 다른 무엇보다 먼저 알아야 한다.
 * 이의는 제재 하나에 한 번이고, 정지 중에도 낼 수 있는 유일한 쓰기다. 답이 오면 같은
 * 자리에 실린다.
 */
export function SanctionBanner() {
  const [sanction, setSanction] = useState<SanctionView | null>(null)
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getMySanction().then(setSanction).catch(() => setSanction(null))
  }, [])

  // 풀렸고 이의의 답도 없는 것은 더 보일 이유가 없다.
  if (!sanction || (!sanction.active && !sanction.appealNote && sanction.kind !== 'WARNING')) return null

  const appeal = async () => {
    setError(null)
    try {
      setSanction(await appealSanction(sanction.id, text))
    } catch (e) {
      setError(e instanceof Error ? e.message : '이의를 내지 못했습니다')
    }
  }

  return (
    <section className={`panel sanction${sanction.active ? ' sanction-active' : ''}`} role="status">
      <p>
        <strong>{KIND_LABEL[sanction.kind]}</strong>
        {sanction.active && sanction.endsAt && <> · {new Date(sanction.endsAt).toLocaleString()} 까지</>}
        {!sanction.active && sanction.liftedAt && ' · 풀렸습니다'}
        {' — '}
        {sanction.reason}
      </p>
      {sanction.appealed ? (
        <p className="small muted">
          이의를 냈습니다.
          {sanction.appealResolution === null && ' 보안 관리자가 봅니다.'}
          {sanction.appealResolution === 'LIFTED' && ' 받아들여져 풀렸습니다.'}
          {sanction.appealResolution === 'UPHELD' && ' 유지됐습니다.'}
          {sanction.appealNote && <> — {sanction.appealNote}</>}
        </p>
      ) : (
        <div className="discussion-compose">
          <textarea
            className="rationale"
            rows={3}
            value={text}
            onChange={(e) => setText(e.target.value)}
            aria-label="이의"
            placeholder="이의 (20자 이상). 발부한 사람이 아닌 다른 보안 관리자가 봅니다"
          />
          <div>
            <button type="button" onClick={() => void appeal()} disabled={text.trim().length < 20}>
              이의 내기
            </button>
          </div>
          {error && <p className="warn small">{error}</p>}
        </div>
      )}
    </section>
  )
}
