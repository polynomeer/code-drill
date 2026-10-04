import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'wouter'
import { ApiFailure, login, register } from '../../api/client'
import type { Session } from '../../api/session'
import { Button, InlineAlert, TextField } from '../../design'
import { AuthLayout } from './AuthLayout'
import styles from './AuthLayout.module.css'

/**
 * 로그인·가입 (기술 설계서 §11.2, 디자인 설계서 §0.1, docs/ui-overhaul.md §6.9).
 *
 * 한 화면에서 모드만 바꾼다. 처음 온 사람에게 가입과 로그인이 어디 있는지 찾게 만들 이유가 없다.
 *
 * 실패 사유를 서버가 나눠 주지 않는다. "없는 계정"과 "틀린 비밀번호"를 구분해 보여 주면 그것만으로
 * 어떤 이메일이 가입돼 있는지 확인할 수 있다 (§11.1). 비밀번호를 잊었으면 재설정으로 간다.
 *
 * 소셜 로그인(GitHub·Google)은 아직 없다 — OAuth 앱 등록이 필요하고, 누를 수 없는 단추를 두지 않는다.
 */
export function SignIn({ onSignedIn }: { onSignedIn: (session: Session, created: boolean) => void }) {
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
      const session = mode === 'login' ? await login(email, password) : await register(email, displayName, password)
      onSignedIn(session, mode === 'register')
    } catch (failure) {
      setError(failure instanceof ApiFailure ? failure.detail.message : '연결하지 못했습니다')
    } finally {
      setBusy(false)
    }
  }

  const switchMode = () => {
    setMode(mode === 'login' ? 'register' : 'login')
    setError(null)
  }

  return (
    <AuthLayout
      title={mode === 'login' ? '로그인' : '가입'}
      lead="실행 증거로 알고리즘 문제 해결 역량을 진단합니다."
      footer={
        <Link href="/problems">로그인 없이 문제 둘러보기</Link>
      }
    >
      <form className={styles.form} onSubmit={submit}>
        <TextField label="이메일" type="email" value={email} autoComplete="email" required onChange={(event) => setEmail(event.target.value)} />
        {mode === 'register' && (
          <TextField
            label="표시 이름"
            hint="비워 두면 이메일 앞부분을 씁니다. 대회 순위표에 보이는 이름입니다."
            value={displayName}
            autoComplete="nickname"
            onChange={(event) => setDisplayName(event.target.value)}
          />
        )}
        <TextField
          label="비밀번호"
          type="password"
          value={password}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          required
          minLength={mode === 'register' ? 10 : undefined}
          hint={mode === 'register' ? '10자 이상' : undefined}
          onChange={(event) => setPassword(event.target.value)}
        />
        {error && <InlineAlert tone="danger">{error}</InlineAlert>}
        <Button type="submit" variant="primary" loading={busy} className={styles.submit}>
          {mode === 'login' ? '로그인' : '가입하고 시작'}
        </Button>
      </form>
      <div className={styles.row}>
        <button type="button" className={styles.switch} onClick={switchMode}>
          {mode === 'login' ? '처음이라면 — 가입하기' : '이미 계정이 있다면 — 로그인'}
        </button>
        {mode === 'login' && <Link href="/forgot-password">비밀번호를 잊었나요?</Link>}
      </div>
    </AuthLayout>
  )
}
