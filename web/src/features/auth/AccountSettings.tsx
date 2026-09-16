import { useEffect, useState } from 'react'
import { getContributions, getMyRating, changePassword, deleteAccount, exportAccount, rename } from '../../api/client'
import type { Session } from '../../api/session'
import type { Contributions, Rating } from '../../shared/types'

/**
 * 계정 설정 (기획서 부록 A 계정 도메인, 기술 설계서 §11.3).
 *
 * 반출과 삭제는 API 로는 있었지만 화면이 없었다. **화면 없는 개인정보 기능은 사용자에게
 * 없는 것과 같다** — "내 데이터를 지워 달라"에 답할 수 있다는 것은 사용자가 스스로 할 수
 * 있다는 뜻이어야 한다.
 *
 * 되돌릴 수 없는 것과 그렇지 않은 것을 화면에서도 나눈다. 아래 위험 구역은 따로 떼어 두고,
 * 삭제는 비밀번호를 다시 받는다.
 */
const TIER_NAME = { NEW: '새 기여자', ACTIVE: '기여자', TRUSTED: '믿을 만한 기여자' }

export function AccountSettings({
  session,
  onClose,
}: {
  session: Session
  onClose: () => void
}) {
  const [name, setName] = useState(session.displayName)
  const [contributions, setContributions] = useState<Contributions | null>(null)
  const [rating, setRating] = useState<Rating | null>(null)
  useEffect(() => {
    getContributions().then(setContributions).catch(() => setContributions(null))
    getMyRating().then(setRating).catch(() => setRating(null))
  }, [])
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [note, setNote] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const run = async (work: () => Promise<string>) => {
    setBusy(true)
    setError(null)
    setNote(null)
    try {
      setNote(await work())
    } catch (e) {
      setError(e instanceof Error ? e.message : '요청에 실패했습니다')
    } finally {
      setBusy(false)
    }
  }

  const download = () =>
    run(async () => {
      const data = await exportAccount()
      // 파일로 준다. 화면에 펼쳐 놓으면 보관할 수도, 옮길 수도 없다.
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
      )
      const link = document.createElement('a')
      link.href = url
      link.download = `codedrill-${session.userId}.json`
      link.click()
      URL.revokeObjectURL(url)
      return '내 데이터를 내려받았습니다.'
    })

  return (
    <section className="panel settings">
      <div className="problem-head">
        <h3>계정 설정</h3>
        <button type="button" className="linklike" onClick={onClose}>
          닫기
        </button>
      </div>

      {error && <p className="warn">{error}</p>}
      {note && <p className="muted">{note}</p>}

      {/* 레이팅 (§8.4). 레이팅 대회가 끝날 때마다 움직인다. 첫 몇 대회는 잠정이다. */}
      {rating && (
        <p className="small">
          레이팅 <strong>{rating.rating}</strong> · 레이팅 대회 {rating.contests}
          {rating.contests > 0 && rating.contests < 5 && <span className="muted"> · 잠정</span>}
        </p>
      )}
      {/* 기여 (§8.5 기여자 평판). 수치는 본인에게만 — 남에게는 글에 실리는 등급뿐이다. */}
      {contributions && (
        <p className="small">
          기여 {contributions.score}점 · {TIER_NAME[contributions.tier]} — 도움됐다 {contributions.helpfulReceived}
          {' · '}풀이 {contributions.solutionsShared} · 답 {contributions.answers} · 세워진 오답 {contributions.donationsApproved}
        </p>
      )}

      <form
        className="settings-block"
        onSubmit={(event) => {
          event.preventDefault()
          void run(async () => {
            await rename(name)
            return '표시 이름을 바꿨습니다.'
          })
        }}
      >
        <label htmlFor="display-name">표시 이름</label>
        <input
          id="display-name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
        />
        {/* 이메일은 바꾸지 않는다. 왜 못 바꾸는지를 적어 두지 않으면 없는 기능처럼 보인다. */}
        <p className="muted small">
          이메일은 로그인 식별자라 여기서 바꾸지 않습니다.
        </p>
        <button type="submit" disabled={busy || name.trim() === ''}>
          이름 저장
        </button>
      </form>

      <form
        className="settings-block"
        onSubmit={(event) => {
          event.preventDefault()
          if (next !== confirm) {
            setError('새 비밀번호가 서로 다릅니다.')
            return
          }
          void run(async () => {
            const { revokedSessions } = await changePassword(current, next)
            setCurrent('')
            setNext('')
            setConfirm('')
            return revokedSessions > 1
              ? `비밀번호를 바꿨습니다. 다른 기기 ${revokedSessions - 1}곳에서 로그아웃됐습니다.`
              : '비밀번호를 바꿨습니다.'
          })
        }}
      >
        <label htmlFor="current-password">지금 비밀번호</label>
        <input
          id="current-password"
          type="password"
          value={current}
          onChange={(event) => setCurrent(event.target.value)}
          required
        />
        <label htmlFor="new-password">새 비밀번호</label>
        <input
          id="new-password"
          type="password"
          value={next}
          onChange={(event) => setNext(event.target.value)}
          required
        />
        <label htmlFor="confirm-password">새 비밀번호 확인</label>
        <input
          id="confirm-password"
          type="password"
          value={confirm}
          onChange={(event) => setConfirm(event.target.value)}
          required
        />
        <p className="muted small">바꾸면 열려 있는 다른 세션이 전부 끊깁니다.</p>
        <button type="submit" disabled={busy || !current || !next}>
          비밀번호 변경
        </button>
      </form>

      <div className="settings-block">
        <h4>내 데이터</h4>
        <p className="muted small">
          계정·제출·초안을 JSON 파일로 내려받습니다 (§11.3).
        </p>
        <button type="button" onClick={() => void download()} disabled={busy}>
          내려받기
        </button>
      </div>

      {/* 되돌릴 수 없는 것은 따로 둔다. 저장 버튼 옆에 있으면 잘못 눌린다. */}
      <div className="settings-block danger">
        <h4>계정 삭제</h4>
        <p className="muted small">
          되돌릴 수 없습니다. 제출한 코드와 초안이 지워지고 판정 이력은 익명으로 남습니다.
        </p>
        <button
          type="button"
          className="destructive"
          disabled={busy}
          onClick={() => {
            const password = window.prompt('계정을 지우려면 비밀번호를 입력하세요.')
            if (!password) return
            void run(async () => {
              await deleteAccount(password)
              return '계정을 지웠습니다.'
            })
          }}
        >
          계정 삭제
        </button>
      </div>
    </section>
  )
}
