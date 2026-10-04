import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'wouter'
import { resetPassword } from '../../api/client'
import { Button, InlineAlert, TextField } from '../../design'
import { AuthLayout } from './AuthLayout'
import styles from './AuthLayout.module.css'

/**
 * 새 비밀번호 `/reset-password?token=`.
 *
 * 바꾸면 열린 세션이 전부 끊긴다(다른 기기 포함) — 재설정의 태반이 "누가 내 계정을 쓰는 것 같다"이다.
 * 그 사실을 성공 문장에 적는다. 토큰은 주소에서 읽고 화면에 다시 보이지 않는다.
 */
export function ResetPasswordPage() {
  const [token] = useState(() => new URLSearchParams(window.location.search).get('token') ?? '')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const reset = useMutation({ mutationFn: () => resetPassword(token, password) })
  const mismatch = confirm.length > 0 && confirm !== password

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (mismatch) return
    reset.mutate()
  }

  if (!token) {
    return (
      <AuthLayout title="링크가 온전하지 않습니다" footer={<Link href="/forgot-password">재설정 다시 요청하기</Link>}>
        <InlineAlert tone="warning">메일의 링크를 끝까지 복사했는지 확인하세요.</InlineAlert>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="새 비밀번호" footer={<Link href="/login">로그인으로 돌아가기</Link>}>
      {reset.isSuccess ? (
        <>
          <InlineAlert tone="success" title="비밀번호를 바꿨습니다">
            다른 기기에서 열려 있던 로그인도 모두 끊었습니다. 새 비밀번호로 로그인하세요.
          </InlineAlert>
          <Link href="/login" className={styles.row}>
            로그인하기
          </Link>
        </>
      ) : (
        <form className={styles.form} onSubmit={submit}>
          <TextField label="새 비밀번호" type="password" hint="10자 이상" minLength={10} required autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} />
          <TextField
            label="한 번 더"
            type="password"
            required
            autoComplete="new-password"
            value={confirm}
            onChange={(event) => setConfirm(event.target.value)}
            error={mismatch ? '위와 같지 않습니다' : undefined}
          />
          {reset.error instanceof Error && (
            <InlineAlert tone="danger" action={<Link href="/forgot-password">다시 요청</Link>}>
              {reset.error.message}
            </InlineAlert>
          )}
          <Button type="submit" variant="primary" loading={reset.isPending} disabled={mismatch} className={styles.submit}>
            바꾸기
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}
