import { Search } from 'lucide-react'
import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode, Ref, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import styles from './Field.module.css'

/**
 * 입력 컨트롤 (디자인 설계서 §14.1 Input/Search, Select).
 *
 * 라벨·도움말·오류를 한 묶음으로 둔다. 오류는 aria-invalid 와 aria-describedby 로 입력에
 * 이어져, 스크린리더가 칸에 들어갈 때 함께 읽는다. 라벨을 숨겨야 하는 자리(검색창)도 이름은
 * 남긴다 — hideLabel.
 */
type FieldChrome = {
  label: ReactNode
  hint?: ReactNode
  error?: ReactNode
  hideLabel?: boolean
}

function Frame({
  id,
  label,
  hint,
  error,
  hideLabel,
  children,
}: FieldChrome & { id: string; children: ReactNode }) {
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={hideLabel ? 'visually-hidden' : styles.label}>
        {label}
      </label>
      {children}
      {error ? (
        <p id={`${id}-message`} className={styles.error}>
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-message`} className={styles.hint}>
            {hint}
          </p>
        )
      )}
    </div>
  )
}

function describe(id: string, chrome: FieldChrome) {
  return {
    'aria-invalid': chrome.error ? true : undefined,
    'aria-describedby': chrome.error || chrome.hint ? `${id}-message` : undefined,
  } as const
}

export function TextField({
  label,
  hint,
  error,
  hideLabel,
  id: given,
  className,
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & FieldChrome & { ref?: Ref<HTMLInputElement> }) {
  const fallback = useId()
  const id = given ?? fallback
  return (
    <Frame id={id} label={label} hint={hint} error={error} hideLabel={hideLabel}>
      <input
        {...rest}
        id={id}
        className={[styles.control, className].filter(Boolean).join(' ')}
        {...describe(id, { label, hint, error })}
      />
    </Frame>
  )
}

export function SearchField({
  label,
  id: given,
  className,
  ...rest
}: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & { label: string; ref?: Ref<HTMLInputElement> }) {
  const fallback = useId()
  const id = given ?? fallback
  return (
    <div className={styles.search}>
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <Search size={16} aria-hidden="true" className={styles.searchIcon} />
      <input {...rest} id={id} type="search" className={[styles.control, styles.searchInput, className].filter(Boolean).join(' ')} />
    </div>
  )
}

export function Textarea({
  label,
  hint,
  error,
  hideLabel,
  id: given,
  className,
  ...rest
}: TextareaHTMLAttributes<HTMLTextAreaElement> & FieldChrome & { ref?: Ref<HTMLTextAreaElement> }) {
  const fallback = useId()
  const id = given ?? fallback
  return (
    <Frame id={id} label={label} hint={hint} error={error} hideLabel={hideLabel}>
      <textarea
        {...rest}
        id={id}
        className={[styles.control, styles.textarea, className].filter(Boolean).join(' ')}
        {...describe(id, { label, hint, error })}
      />
    </Frame>
  )
}

export function Select({
  label,
  hint,
  error,
  hideLabel,
  id: given,
  className,
  children,
  ...rest
}: SelectHTMLAttributes<HTMLSelectElement> & FieldChrome & { ref?: Ref<HTMLSelectElement> }) {
  const fallback = useId()
  const id = given ?? fallback
  return (
    <Frame id={id} label={label} hint={hint} error={error} hideLabel={hideLabel}>
      <select
        {...rest}
        id={id}
        className={[styles.control, styles.select, className].filter(Boolean).join(' ')}
        {...describe(id, { label, hint, error })}
      >
        {children}
      </select>
    </Frame>
  )
}
