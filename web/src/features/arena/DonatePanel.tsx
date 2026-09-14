import { useEffect, useState } from 'react'
import { donateToArena, getMyDonations } from '../../api/client'
import type { ArenaDonation, Submission } from '../../shared/types'

const STATUS_LABEL: Record<ArenaDonation['status'], string> = {
  PENDING: '검수 대기',
  APPROVED: '세워짐',
  REJECTED: '반려',
  RETIRED: '내려짐',
}

/**
 * 내 오답을 아레나에 내놓는다 (기획서 §8.3 "익명화된 오답", §8.5 검수).
 *
 * 틀린 제출의 판정 아래에 나온다. 내놓는 것과 깨뜨리는 것은 다른 일이라, 이 문제를
 * 맞히지 않았어도 내놓을 수 있다 — 틀린 사람이야말로 내놓을 것이 있다.
 *
 * 이름은 어디에도 나가지 않는다. 검수자가 보고 세우면 과녁이 되고, 그때부터 맞힌 사람들이
 * 이것을 깨뜨린다. 반려와 내림의 사유는 여기 돌아온다.
 */
export function DonatePanel({ submission }: { submission: Submission }) {
  const [mine, setMine] = useState<ArenaDonation[] | null>(null)
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setMine(null)
    setNote('')
    setError(null)
    getMyDonations(submission.problemId).then(setMine).catch(() => setMine([]))
  }, [submission.id, submission.problemId])

  // 아레나가 돌리는 오답은 Kotlin 이다 — 저작자의 대표 오답과 같은 길로 컴파일한다.
  if (submission.language !== 'KOTLIN' || submission.verdict !== 'WRONG_ANSWER' || mine === null) return null

  const existing = mine.find((d) => d.submissionId === submission.id)

  const donate = async () => {
    setError(null)
    try {
      const donation = await donateToArena(submission.problemId, submission.id, note)
      setMine([donation, ...mine])
    } catch (e) {
      setError(e instanceof Error ? e.message : '내놓지 못했습니다')
    }
  }

  return (
    <div className="arena-donate">
      <h4>이 오답을 아레나에 내놓기</h4>
      {existing ? (
        <p className="small">
          {STATUS_LABEL[existing.status]}
          {existing.status === 'APPROVED' && <> — 과녁 <code className="mono">{existing.targetName}</code></>}
          {existing.reason && <> · {existing.reason}</>}
        </p>
      ) : (
        <>
          <p className="muted small">
            검수를 거쳐 익명의 과녁이 됩니다. 이 문제를 맞힌 사람들이 이 코드를 틀리게 만드는 입력을 찾습니다.
            이름은 어디에도 나가지 않습니다.
          </p>
          <input
            className="rationale"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            aria-label="무엇을 잘못했는지"
            placeholder="무엇을 잘못했는지 한 줄 (10자 이상)"
          />
          <button type="button" onClick={() => void donate()} disabled={note.trim().length < 10}>
            내놓기
          </button>
          {error && <p className="warn small">{error}</p>}
        </>
      )}
    </div>
  )
}
