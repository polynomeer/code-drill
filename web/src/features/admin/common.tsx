import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { QueryKey } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Button, Dialog, InlineAlert, Textarea, useToast } from '../../design'
import { fullTime, relativeTime } from '../../shared/time'
import styles from './AdminPage.module.css'

/**
 * 운영 결정 하나를 부른다. 성공하면 알림과 함께 큐를 다시 읽고, 거절(2인 원칙·이미 결정됨)은 던져서
 * 결정 대화상자가 그 사유를 그대로 보이게 한다 — 운영자가 "왜 안 되나"를 서버 문장으로 읽어야 한다.
 */
export function useDecision<T>(fn: (input: T) => Promise<unknown>, done: string, refresh: QueryKey[]) {
  const client = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      toast.show(done, 'success')
      for (const key of refresh) void client.invalidateQueries({ queryKey: key })
    },
  })
}

/**
 * 결정 대화상자. 무엇을 하는지(제목), 무엇이 바뀌는지(설명), 왜 하는지(사유 — 감사 로그에 남는다)를
 * 받는다. 되돌릴 수 없는 결정은 `danger` 로 단추 색이 바뀐다.
 */
export function DecisionDialog({
  open,
  onClose,
  title,
  description,
  confirm,
  danger = false,
  reasonLabel,
  reasonRequired = true,
  pending,
  error,
  onConfirm,
  children,
}: {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  confirm: string
  danger?: boolean
  /** 없으면 사유 칸이 없다 (승인처럼 사유가 감사 로그에 따로 남지 않는 결정) */
  reasonLabel?: string
  reasonRequired?: boolean
  pending: boolean
  error: unknown
  onConfirm: (reason: string) => void
  children?: ReactNode
}) {
  const [reason, setReason] = useState('')
  const submit = (event: FormEvent) => {
    event.preventDefault()
    onConfirm(reason.trim())
  }
  const blocked = reasonLabel !== undefined && reasonRequired && reason.trim().length === 0

  return (
    <Dialog open={open} onClose={onClose} title={title} description={description}>
      <form className={styles.dialogForm} onSubmit={submit}>
        {children}
        {reasonLabel && (
          <Textarea
            label={reasonLabel}
            hint={reasonRequired ? '감사 로그에 남습니다.' : '선택. 감사 로그에 남습니다.'}
            rows={3}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        )}
        {error instanceof Error && <InlineAlert tone="danger">{error.message}</InlineAlert>}
        <div className={styles.dialogActions}>
          <Button type="button" variant="tertiary" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" variant={danger ? 'danger' : 'primary'} loading={pending} disabled={blocked}>
            {confirm}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}

/** 계정·제출 id. 앞 여덟 글자만 보이고 전체는 툴팁과 복사로 — 표가 UUID 로 덮이지 않게. */
export function Id({ value, mine }: { value: string | null | undefined; mine?: string }) {
  if (!value) return <span className={styles.muted}>—</span>
  return (
    <code className={styles.id} title={value}>
      {value.length > 12 ? value.slice(0, 8) : value}
      {mine && value === mine && <span className={styles.me}> (나)</span>}
    </code>
  )
}

export function When({ iso }: { iso: string }) {
  return (
    <time dateTime={iso} title={fullTime(iso)} className={styles.when}>
      {relativeTime(iso)}
    </time>
  )
}

/** 큐가 비었을 때. 운영자에게 "할 일 없음"은 좋은 소식이다 — 그렇게 말한다. */
export function Clear({ children }: { children: ReactNode }) {
  return <p className={styles.clear}>{children}</p>
}
