/**
 * 외관 테마 (디자인 설계서 §11.2 Appearance — Light / Dark / System).
 *
 * 선택은 기기마다 다를 수 있어 localStorage 에 둔다. 첫 페인트 전 적용은 index.html 의
 * 인라인 스크립트가 같은 키로 한다 — 여기서만 하면 다크 사용자가 흰 화면을 한 번 본다.
 * 키나 해석 규칙을 바꾸면 그 스크립트도 함께 고친다.
 */
import { useSyncExternalStore } from 'react'

export type ThemePreference = 'light' | 'dark' | 'system'

const KEY = 'codedrill.theme'
const media = window.matchMedia('(prefers-color-scheme: dark)')

export function getThemePreference(): ThemePreference {
  try {
    const raw = localStorage.getItem(KEY)
    return raw === 'light' || raw === 'dark' ? raw : 'system'
  } catch {
    return 'system'
  }
}

function resolve(preference: ThemePreference): 'light' | 'dark' {
  if (preference !== 'system') return preference
  return media.matches ? 'dark' : 'light'
}

function apply(preference: ThemePreference) {
  document.documentElement.dataset.theme = resolve(preference)
}

export function setThemePreference(preference: ThemePreference) {
  try {
    if (preference === 'system') localStorage.removeItem(KEY)
    else localStorage.setItem(KEY, preference)
  } catch {
    // 저장하지 못해도 이번 탭에는 적용한다.
  }
  apply(preference)
}

// 시스템을 따르는 동안 OS 설정이 바뀌면 바로 따라간다.
media.addEventListener('change', () => {
  if (getThemePreference() === 'system') apply('system')
})

/** 지금 그려진 테마. `<html data-theme>` 이 단일 출처다 — 선택·시스템 변경·첫 페인트 스크립트 모두 거기 쓴다 */
export function getAppTheme(): 'light' | 'dark' {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export function useAppTheme(): 'light' | 'dark' {
  return useSyncExternalStore((listener) => {
    const observer = new MutationObserver(listener)
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
    return () => observer.disconnect()
  }, getAppTheme)
}
