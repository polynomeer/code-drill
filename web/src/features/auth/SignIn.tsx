import { useState, type FormEvent } from 'react'
import { ApiFailure, login, register } from '../../api/client'
import type { Session } from '../../api/session'

/**
 * 로그인·가입 (기술 설계서 §11.2, 디자인 설계서 §0.1).
 *
 * 한 화면에서 모드만 바꾼다. 처음 온 사람에게 가입과 로그인이 어디 있는지 찾게 만들
 * 이유가 없다.
 *
 * 실패 사유를 서버가 나눠 주지 않는다. "없는 계정"과 "틀린 비밀번호"를 구분해 보여
 * 주면 그것만으로 어떤 이메일이 가입돼 있는지 확인할 수 있다 (§11.1).
 */
export function SignIn({ onSignedIn }: { onSignedIn: (session: Session) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const session =
        mode === 'login'
          ? await login(email, password)
          : await register(email, displayName, password)
      onSignedIn(session)
    } catch (failure) {
      setError(
        failure instanceof ApiFailure ? failure.detail.message : '연결하지 못했다',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="signin">
      <form className="signin-card" onSubmit={submit}>
        <h1>CodeDrill</h1>
        <p className="signin-lead">
          실행 증거로 알고리즘 문제 해결 역량을 진단한다.
        </p>

        <label>
          이메일
          <input
            type="email"
            value={email}
            autoComplete="email"
            required
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>

        {mode === 'register' && (
          <label>
            표시 이름
            <input
              type="text"
              value={displayName}
              autoComplete="nickname"
              placeholder="비워 두면 이메일 앞부분을 쓴다"
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </label>
        )}

        <label>
          비밀번호
          <input
            type="password"
            value={password}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            required
            minLength={mode === 'register' ? 10 : undefined}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {mode === 'register' && <p className="signin-hint">10자 이상</p>}

        {error && <p className="signin-error">{error}</p>}

        <button type="submit" disabled={busy}>
          {busy ? '보내는 중…' : mode === 'login' ? '로그인' : '가입하고 시작'}
        </button>

        <button
          type="button"
          className="signin-switch"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError(null)
          }}
        >
          {mode === 'login' ? '처음이라면 — 가입하기' : '이미 계정이 있다면 — 로그인'}
        </button>
      </form>
    </div>
  )
}
