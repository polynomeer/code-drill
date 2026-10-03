import { CircleAlert, CircleCheck, Info, TriangleAlert, X } from 'lucide-react'
import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import styles from './Feedback.module.css'

export type Tone = 'info' | 'success' | 'warning' | 'danger'

const TONE_ICON = {
  info: Info,
  success: CircleCheck,
  warning: TriangleAlert,
  danger: CircleAlert,
} as const

/**
 * 화면 안에 머무는 알림. 오류와 복구 행동은 사용자가 처리할 때까지 남는다
 * (디자인 설계서 §14.3). danger 는 role=alert 로 바로 읽힌다.
 */
export function InlineAlert({
  tone = 'info',
  title,
  children,
  action,
}: {
  tone?: Tone
  title?: ReactNode
  children?: ReactNode
  action?: ReactNode
}) {
  const Icon = TONE_ICON[tone]
  return (
    <div className={`${styles.alert} ${styles[tone]}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <Icon size={16} aria-hidden="true" className={styles.alertIcon} />
      <div className={styles.alertBody}>
        {title && <p className={styles.alertTitle}>{title}</p>}
        {children && <div>{children}</div>}
      </div>
      {action && <div className={styles.alertAction}>{action}</div>}
    </div>
  )
}

/**
 * 빈 상태. 왜 비었는지와 다음 행동을 함께 준다 — "없습니다"만 있으면 막다른 길이다
 * (UI 디자인 문서 §3.3 빈 결과).
 */
export function EmptyState({
  icon,
  title,
  children,
  action,
}: {
  icon?: ReactNode
  title: ReactNode
  children?: ReactNode
  action?: ReactNode
}) {
  return (
    <div className={styles.empty}>
      {icon && (
        <span className={styles.emptyIcon} aria-hidden="true">
          {icon}
        </span>
      )}
      <p className={styles.emptyTitle}>{title}</p>
      {children && <div className={styles.emptyBody}>{children}</div>}
      {action && <div className={styles.emptyAction}>{action}</div>}
    </div>
  )
}

/* ─── Toast ─────────────────────────────────────────────────────────────── */

type ToastItem = { id: number; tone: Tone; message: ReactNode }
type ToastApi = { show: (message: ReactNode, tone?: Tone) => void }

const ToastContext = createContext<ToastApi | null>(null)

/**
 * 짧은 알림 — 저장·복사·일시적 오류에만 쓴다. 판정 결과는 토스트로 대신하지 않는다
 * (UI 디자인 문서 §7.3). 사라지는 알림이라 놓치면 안 되는 일에는 InlineAlert 를 쓴다.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const next = useRef(0)

  const dismiss = useCallback((id: number) => setItems((all) => all.filter((item) => item.id !== id)), [])

  const show = useCallback(
    (message: ReactNode, tone: Tone = 'info') => {
      const id = ++next.current
      setItems((all) => [...all.slice(-2), { id, tone, message }])
      // 오류는 사용자가 닫을 때까지 둔다 (디자인 설계서 §14.3).
      if (tone !== 'danger') setTimeout(() => dismiss(id), 4000)
    },
    [dismiss],
  )

  const api = useMemo(() => ({ show }), [show])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className={styles.toasts} aria-live="polite" aria-relevant="additions">
        {items.map((item) => {
          const Icon = TONE_ICON[item.tone]
          return (
            <div key={item.id} className={`${styles.toast} ${styles[item.tone]}`}>
              <Icon size={16} aria-hidden="true" className={styles.alertIcon} />
              <span className={styles.toastMessage}>{item.message}</span>
              <button type="button" className={styles.toastClose} aria-label="알림 닫기" onClick={() => dismiss(item.id)}>
                <X size={14} aria-hidden="true" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const api = useContext(ToastContext)
  if (!api) throw new Error('useToast 는 ToastProvider 안에서만 쓴다')
  return api
}
