import { useCallback, useEffect, useRef, useState } from 'react'
import { setParam } from '../../shared/url'
import { SPEEDS, intervalOf, nextPlayStep } from './timeline'
import type { Marker, Speed } from './timeline'
import { track } from '../../shared/analytics'
import type { SeekMethod } from '../../shared/analytics'

const SPEED_KEY = 'codedrill.replay.speed'
const MODE_KEY = 'codedrill.replay.mode'

export type TimelineMode = 'summary' | 'all'

/**
 * 재생 위치 하나 (UI 디자인 문서 §5.3, §11.2 "같은 seq 에서 네 Pane 상태 일치").
 *
 * 네 pane 은 걸음을 각자 들지 않고 여기서 받는다. 걸음은 주소의 `?step=` 에도 적는다 — 그 주소를
 * 붙이면 받는 사람이 같은 걸음에서 연다 (게시판의 리플레이 붙임과 같은 약속).
 *
 * 사용자가 걸음을 직접 옮기면(스크럽·코드 줄·캔버스·키보드) 재생을 멈춘다 (디자인 설계서 §8.3
 * "재생 중 사용자가 코드를 스크롤하거나 상태를 선택하면 자동 재생을 일시 정지").
 */
export function useReplayPlayer({
  total,
  markers,
  initialStep,
}: {
  total: number
  markers: Marker[]
  initialStep: number
}) {
  const [step, setStepState] = useState(() => clamp(initialStep, total))
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeedState] = useState<Speed>(() => stored(SPEED_KEY, SPEEDS, 1))
  const [mode, setModeState] = useState<TimelineMode>(() => stored(MODE_KEY, ['summary', 'all'] as const, 'summary'))

  // total 이 늦게 오면(manifest) 다시 맞춘다
  useEffect(() => setStepState((current) => clamp(current, total)), [total])

  useEffect(() => setParam('step', String(step)), [step])

  const scrubbing = useRef<{ from: number; timer: ReturnType<typeof setTimeout> } | null>(null)

  /**
   * 사용자가 옮긴 걸음. 재생을 멈춘다. [method] 는 어떻게 옮겼나 — 스크러버·키·단추·코드 줄·마커·분기·목록
   * (§16.1 replay_seeked). 스크러버는 끄는 동안 수십 번 바뀌므로 멈춘 뒤 한 번으로 센다.
   */
  const seek = useCallback(
    (next: number, method: SeekMethod = 'button') => {
      setPlaying(false)
      const to = clamp(next, total)
      const from = latest.current.step
      setStepState(to)
      if (method === 'scrub') {
        const start = scrubbing.current?.from ?? from
        if (scrubbing.current) clearTimeout(scrubbing.current.timer)
        scrubbing.current = {
          from: start,
          timer: setTimeout(() => {
            scrubbing.current = null
            if (start !== to) track('replay_seeked', { from: start, to, method })
          }, 500),
        }
      } else if (to !== from) {
        track('replay_seeked', { from, to, method })
      }
    },
    [total],
  )

  const latest = useRef({ step, markers, total, mode })
  latest.current = { step, markers, total, mode }

  useEffect(() => {
    if (!playing) return
    const timer = window.setInterval(() => {
      const { step: now, markers: marks, total: end, mode: m } = latest.current
      const next = nextPlayStep(marks, now, end, m)
      if (next === null) {
        setPlaying(false)
        return
      }
      setStepState(next)
      if (next >= end) setPlaying(false)
    }, intervalOf(speed))
    return () => window.clearInterval(timer)
  }, [playing, speed])

  const toggle = useCallback(() => {
    setPlaying((current) => {
      // 끝에서 재생을 누르면 처음부터 다시 — 아무 일도 안 일어나는 버튼은 고장 난 버튼이다
      if (!current && latest.current.step >= latest.current.total) setStepState(0)
      return !current
    })
  }, [])

  const setSpeed = useCallback((next: Speed) => {
    setSpeedState(next)
    store(SPEED_KEY, String(next))
  }, [])

  const setMode = useCallback((next: TimelineMode) => {
    setModeState(next)
    store(MODE_KEY, next)
  }, [])

  return { step, seek, playing, toggle, pause: () => setPlaying(false), speed, setSpeed, mode, setMode }
}

function clamp(step: number, total: number): number {
  if (!Number.isFinite(step)) return 0
  return Math.max(0, Math.min(total, Math.round(step)))
}

function stored<T extends string | number>(key: string, allowed: readonly T[], fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return allowed.find((value) => String(value) === raw) ?? fallback
  } catch {
    return fallback
  }
}

function store(key: string, value: string) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // 사생활 보호 모드 — 기억하지 못할 뿐 재생은 된다
  }
}
