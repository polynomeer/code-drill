import { useEffect, useState } from 'react'

/**
 * 1초마다 바뀌는 지금 (카운트다운). 셀 것이 없으면 멈춘다 — 끝난 대회만 보는 화면이 매초 다시
 * 그려질 이유가 없다.
 */
export function useNow(ticking: boolean): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!ticking) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [ticking])
  return now
}
