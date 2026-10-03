import { X } from 'lucide-react'
import { useEffect, useId, useRef } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import styles from './Structure.module.css'

/**
 * 구조 컴포넌트 (디자인 설계서 §14.1 Tabs·Dialog).
 *
 * Dialog 는 브라우저의 <dialog> 를 쓴다. showModal() 이 포커스 가두기·Esc·배경 비활성을
 * 주므로 따로 짓지 않는다. 닫을 때 연 요소로 포커스를 돌리는 것만 더한다 (§14.3).
 */

/* ─── Panel ─── */

export function Panel({
  title,
  actions,
  children,
  className,
  labelledBy,
}: {
  title?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
  /** 제목 대신 다른 요소로 이름을 줄 때 */
  labelledBy?: string
}) {
  const headingId = useId()
  return (
    <section
      className={[styles.panel, className].filter(Boolean).join(' ')}
      aria-labelledby={labelledBy ?? (title ? headingId : undefined)}
    >
      {(title || actions) && (
        <header className={styles.panelHeader}>
          {title && (
            <h2 id={headingId} className={styles.panelTitle}>
              {title}
            </h2>
          )}
          {actions && <div className={styles.panelActions}>{actions}</div>}
        </header>
      )}
      {children}
    </section>
  )
}

/* ─── Tabs ─── */

export type TabItem<K extends string> = { key: K; label: ReactNode; disabled?: boolean }

/**
 * 탭 (WAI-ARIA Tabs 패턴). 화살표로 옮기고 Home/End 로 끝으로 간다. 탭 목록에는 탭 하나만
 * 탭 순서에 들어간다 — 탭 열 개를 Tab 키로 다 지나야 본문에 닿으면 키보드 사용자가 지친다.
 */
export function Tabs<K extends string>({
  label,
  items,
  value,
  onChange,
  children,
}: {
  label: string
  items: TabItem<K>[]
  value: K
  onChange: (key: K) => void
  /** 지금 고른 탭의 본문 */
  children: ReactNode
}) {
  const base = useId()
  const list = useRef<HTMLDivElement>(null)
  const enabled = items.filter((item) => !item.disabled)

  const move = (event: KeyboardEvent) => {
    const index = enabled.findIndex((item) => item.key === value)
    const target =
      event.key === 'ArrowRight'
        ? enabled[(index + 1) % enabled.length]
        : event.key === 'ArrowLeft'
          ? enabled[(index - 1 + enabled.length) % enabled.length]
          : event.key === 'Home'
            ? enabled[0]
            : event.key === 'End'
              ? enabled[enabled.length - 1]
              : undefined
    if (!target) return
    event.preventDefault()
    onChange(target.key)
    list.current?.querySelector<HTMLButtonElement>(`[data-key="${target.key}"]`)?.focus()
  }

  return (
    <div className={styles.tabs}>
      <div role="tablist" aria-label={label} className={styles.tabList} ref={list} onKeyDown={move}>
        {items.map((item) => {
          const selected = item.key === value
          return (
            <button
              key={item.key}
              type="button"
              role="tab"
              id={`${base}-tab-${item.key}`}
              data-key={item.key}
              aria-selected={selected}
              aria-controls={`${base}-panel`}
              tabIndex={selected ? 0 : -1}
              disabled={item.disabled}
              className={styles.tab}
              onClick={() => onChange(item.key)}
            >
              {item.label}
            </button>
          )
        })}
      </div>
      <div
        role="tabpanel"
        id={`${base}-panel`}
        aria-labelledby={`${base}-tab-${value}`}
        tabIndex={0}
        className={styles.tabPanel}
      >
        {children}
      </div>
    </div>
  )
}

/* ─── Dialog ─── */

export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'default',
}: {
  open: boolean
  onClose: () => void
  title: ReactNode
  description?: ReactNode
  children?: ReactNode
  footer?: ReactNode
  size?: 'default' | 'wide'
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const opener = useRef<Element | null>(null)
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      opener.current = document.activeElement
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  // 닫히면 연 요소로 포커스를 돌려준다. 언마운트로 사라질 때도 마찬가지다.
  useEffect(() => {
    if (open) return
    const target = opener.current
    opener.current = null
    if (target instanceof HTMLElement) target.focus()
  }, [open])

  return (
    <dialog
      ref={ref}
      className={`${styles.dialog} ${size === 'wide' ? styles.wide : ''}`}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      // Esc 는 브라우저가 닫는다. 상태도 따라 닫혀야 다음 열기가 동작한다.
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      // 바깥(배경)을 누르면 닫는다. 대화상자 안을 누른 것은 target 이 dialog 가 아니다.
      onClick={(event) => {
        if (event.target === ref.current) onClose()
      }}
    >
      {open && (
        <div className={styles.dialogInner}>
          <header className={styles.dialogHeader}>
            <div>
              <h2 id={titleId} className={styles.dialogTitle}>
                {title}
              </h2>
              {description && (
                <p id={descriptionId} className={styles.dialogDescription}>
                  {description}
                </p>
              )}
            </div>
            <button type="button" className={styles.dialogClose} aria-label="닫기" onClick={onClose}>
              <X size={18} aria-hidden="true" />
            </button>
          </header>
          <div className={styles.dialogBody}>{children}</div>
          {footer && <footer className={styles.dialogFooter}>{footer}</footer>}
        </div>
      )}
    </dialog>
  )
}
