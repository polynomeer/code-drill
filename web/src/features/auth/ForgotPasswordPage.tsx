import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'wouter'
import { forgotPassword } from '../../api/client'
import { Button, InlineAlert, TextField } from '../../design'
import { AuthLayout } from './AuthLayout'
import styles from './AuthLayout.module.css'

/**
 * 비밀번호 재설정 요청 `/forgot-password`.
 *
 * 보낸 뒤의 문장은 계정이 있든 없든 같다 — "그 이메일로 가입했다면 링크를 보냈습니다". 다르게 말하면
 * 이 화면이 어떤 이메일이 가입돼 있는지 알려 주는 창구가 된다 (서버도 같은 답을 한다).
 */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const request = useMutation({ mutationFn: () => forgotPassword(email.trim()) })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    request.mutate()
  }

  return (
    <AuthLayout title="비밀번호 재설정" lead="가입한 이메일을 적으면 재설정 링크를 보냅니다. 링크는 30분 동안 한 번 쓸 수 있습니다." footer={<Link href="/login">로그인으로 돌아가기</Link>}>
      {request.isSuccess ? (
        <InlineAlert tone="success" title="확인하세요">
          {email.trim()} 로 가입했다면 재설정 링크를 보냈습니다. 메일이 오지 않으면 주소를 확인하고 잠시 뒤 다시 요청하세요.
        </InlineAlert>
      ) : (
        <form className={styles.form} onSubmit={submit}>
          <TextField label="이메일" type="email" value={email} autoComplete="email" required onChange={(event) => setEmail(event.target.value)} />
          {request.error instanceof Error && <InlineAlert tone="danger">{request.error.message}</InlineAlert>}
          <Button type="submit" variant="primary" loading={request.isPending} className={styles.submit}>
            재설정 링크 받기
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}
