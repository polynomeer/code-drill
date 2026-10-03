import type { ButtonHTMLAttributes, ReactNode, Ref } from 'react'
import { Spinner } from './Spinner'
import styles from './Button.module.css'

/**
 * 행동 우선순위 (디자인 설계서 §6.2, UI 디자인 문서 §7.1).
 *
 * primary 는 한 그룹에 하나다. danger 는 되돌릴 수 없는 행동에만 쓰고, 단독 tertiary 로
 * 위험 행동을 두지 않는다. loading 은 disabled 와 다르게 보인다 — 눌렸고 기다리는 중이다.
 */
export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'danger'
export type ButtonSize = 'dense' | 'default'

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
  /** 글자 앞 아이콘. 장식이므로 읽히지 않는다 */
  icon?: ReactNode
  ref?: Ref<HTMLButtonElement>
}

export function Button({
  variant = 'secondary',
  size = 'default',
  loading = false,
  icon,
  disabled,
  className,
  children,
  type = 'button',
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      type={type}
      className={[styles.button, styles[variant], styles[size], className].filter(Boolean).join(' ')}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {loading ? <Spinner size={14} /> : icon && <span className={styles.icon} aria-hidden="true">{icon}</span>}
      {children}
    </button>
  )
}

/**
 * 아이콘만 있는 버튼. 이름이 없으면 스크린리더에는 "버튼"뿐이므로 label 이 필수이고,
 * 같은 글을 툴팁으로도 보여준다 (디자인 설계서 §13.4).
 */
export function IconButton({
  label,
  icon,
  size = 'default',
  className,
  type = 'button',
  ...rest
}: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  label: string
  icon: ReactNode
  size?: ButtonSize
  ref?: Ref<HTMLButtonElement>
}) {
  return (
    <button
      {...rest}
      type={type}
      aria-label={label}
      title={rest.title ?? label}
      className={[styles.button, styles.tertiary, styles.iconOnly, styles[size], className]
        .filter(Boolean)
        .join(' ')}
    >
      <span aria-hidden="true" className={styles.icon}>
        {icon}
      </span>
    </button>
  )
}
