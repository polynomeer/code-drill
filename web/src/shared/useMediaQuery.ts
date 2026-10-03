import { useSyncExternalStore } from 'react'

/** 미디어 쿼리가 지금 맞는지. 화면 폭이 중단점을 넘으면 다시 그린다. */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (listener) => {
      const media = window.matchMedia(query)
      media.addEventListener('change', listener)
      return () => media.removeEventListener('change', listener)
    },
    () => window.matchMedia(query).matches,
  )
}
