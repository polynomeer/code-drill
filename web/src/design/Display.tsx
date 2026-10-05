import {
  Braces,
  CircleCheck,
  CircleX,
  Clock,
  FileOutput,
  Layers,
  MemoryStick,
  Play,
  ServerCrash,
  SquareTerminal,
  TimerOff,
  TriangleAlert,
} from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { DIFFICULTIES, DIFFICULTY_LABEL, VERDICT_LABEL } from '../shared/types'
import type { Difficulty, SubmissionStatus, Verdict } from '../shared/types'
import styles from './Display.module.css'

/**
 * 표시 컴포넌트 (디자인 설계서 §14.1 Badge, UI 디자인 문서 §7.1 Filter Chip).
 *
 * 의미가 있는 배지는 색만으로 말하지 않는다 — 아이콘과 글자가 함께 간다 (§15.2).
 */
export type BadgeTone = 'neutral' | 'brand' | 'trace' | 'success' | 'warning' | 'danger' | 'system'

export function Badge({
  tone = 'neutral',
  icon,
  children,
  className,
}: {
  tone?: BadgeTone
  icon?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <span className={[styles.badge, styles[tone], className].filter(Boolean).join(' ')}>
      {icon && (
        <span className={styles.badgeIcon} aria-hidden="true">
          {icon}
        </span>
      )}
      {children}
    </span>
  )
}

/** 난이도 배지. 단계 수를 막대로도 보여 색을 못 보는 사용자도 높낮이를 읽는다. */
export function DifficultyBadge({ level }: { level: Difficulty }) {
  const rank = DIFFICULTIES.indexOf(level) + 1
  return (
    <span className={styles.difficulty} style={{ color: `var(--color-difficulty-${rank})` }}>
      <span className={styles.bars} aria-hidden="true">
        {DIFFICULTIES.map((_, index) => (
          <span key={index} className={index < rank ? styles.barOn : styles.bar} />
        ))}
      </span>
      {DIFFICULTY_LABEL[level]}
    </span>
  )
}

/** 판정 표현 (UI 디자인 문서 §7.2, 디자인 설계서 §7.2). */
const VERDICT_STYLE: Record<Verdict, { tone: BadgeTone; Icon: typeof CircleCheck }> = {
  ACCEPTED: { tone: 'success', Icon: CircleCheck },
  WRONG_ANSWER: { tone: 'danger', Icon: CircleX },
  COMPILE_ERROR: { tone: 'danger', Icon: TriangleAlert },
  RUNTIME_ERROR: { tone: 'danger', Icon: SquareTerminal },
  TIME_LIMIT: { tone: 'warning', Icon: TimerOff },
  MEMORY_LIMIT: { tone: 'warning', Icon: MemoryStick },
  OUTPUT_LIMIT: { tone: 'warning', Icon: FileOutput },
  SYSTEM_ERROR: { tone: 'system', Icon: ServerCrash },
}

export function VerdictBadge({ verdict }: { verdict: Verdict }) {
  const { tone, Icon } = VERDICT_STYLE[verdict]
  return (
    <Badge tone={tone} icon={<Icon size={14} />}>
      {VERDICT_LABEL[verdict]}
    </Badge>
  )
}

/** 진행 중 상태 (UI 디자인 문서 §7.2 상단 네 줄). LEASED 는 사용자에게 QUEUED 와 같다. */
const STATUS_STYLE: Partial<Record<SubmissionStatus, { tone: BadgeTone; Icon: typeof Clock; label: string }>> = {
  CREATED: { tone: 'neutral', Icon: Clock, label: '채점 대기 중' },
  QUEUED: { tone: 'neutral', Icon: Clock, label: '채점 대기 중' },
  LEASED: { tone: 'neutral', Icon: Clock, label: '채점 대기 중' },
  COMPILING: { tone: 'brand', Icon: Braces, label: '컴파일 중' },
  RUNNING: { tone: 'brand', Icon: Play, label: '테스트 실행 중' },
  AGGREGATING: { tone: 'trace', Icon: Layers, label: '결과 정리 중' },
}

export function JudgeStatusBadge({ status }: { status: SubmissionStatus }) {
  const style = STATUS_STYLE[status]
  if (!style) return null
  return (
    <Badge tone={style.tone} icon={<style.Icon size={14} />}>
      {style.label}
    </Badge>
  )
}

/**
 * 필터 칩. 켜짐을 색·테두리·글자 굵기로 함께 바꾸고 aria-pressed 로 알린다
 * (UI 디자인 문서 §3.3).
 */
export function Chip({
  pressed,
  count,
  children,
  className,
  ...rest
}: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'aria-pressed'> & {
  pressed: boolean
  count?: number
}) {
  return (
    <button
      type="button"
      {...rest}
      aria-pressed={pressed}
      className={[styles.chip, pressed && styles.chipOn, className].filter(Boolean).join(' ')}
    >
      {children}
      {count !== undefined && <span className={styles.chipCount}>{count}</span>}
    </button>
  )
}

/** 단축키 표기 */
export function Kbd({ children }: { children: ReactNode }) {
  return <kbd className={styles.kbd}>{children}</kbd>
}

/** 진행 막대. 값은 접근성 API 에도 나간다. */
export function ProgressBar({
  value,
  max = 100,
  label,
  tone = 'brand',
}: {
  value: number
  max?: number
  label: string
  tone?: 'brand' | 'success' | 'warning' | 'danger'
}) {
  const ratio = max > 0 ? Math.min(1, Math.max(0, value / max)) : 0
  return (
    <span
      className={styles.progress}
      role="progressbar"
      aria-label={label}
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={max}
    >
      <span className={styles.progressFill} style={{ width: `${ratio * 100}%`, backgroundColor: `var(--color-${tone})` }} />
    </span>
  )
}
