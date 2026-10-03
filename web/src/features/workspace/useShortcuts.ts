import { useEffect, useRef } from 'react'

/**
 * Workspace 단축키 (디자인 설계서 §6.4, UI 디자인 문서 §4.4).
 *
 * window 의 **capture 단계**에서 듣는다. Monaco 는 ⌘↵ 를 "아래에 줄 넣기"로 쓰고 이벤트를
 * 삼키므로, 버블 단계에서는 에디터에 포커스가 있을 때 단축키가 죽는다 — 사용자가 가장 많이
 * 누르는 순간이 바로 그때다.
 *
 * 숫자 키는 `event.code` 로 본다. macOS 에서 ⌥1 의 `event.key` 는 "¡" 다.
 */
export type ShortcutHandlers = {
  run: () => void
  submit: () => void
  toggleDrawer: () => void
  toggleProblemPane: () => void
  focusProblem: () => void
  focusEditor: () => void
}

export const IS_MAC = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform)
export const MOD = IS_MAC ? '⌘' : 'Ctrl'
export const ALT = IS_MAC ? '⌥' : 'Alt'

export function useShortcuts(handlers: ShortcutHandlers) {
  // 매 렌더마다 리스너를 다시 달지 않도록 최신 핸들러는 ref 로 본다.
  const latest = useRef(handlers)
  latest.current = handlers

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const mod = IS_MAC ? event.metaKey : event.ctrlKey
      const h = latest.current
      let action: (() => void) | null = null

      if (mod && !event.altKey && event.key === 'Enter') action = event.shiftKey ? h.submit : h.run
      else if (mod && !event.shiftKey && !event.altKey && event.code === 'KeyJ') action = h.toggleDrawer
      else if (mod && !event.shiftKey && !event.altKey && event.code === 'Backslash') action = h.toggleProblemPane
      else if (event.altKey && !mod && !event.shiftKey && event.code === 'Digit1') action = h.focusProblem
      else if (event.altKey && !mod && !event.shiftKey && event.code === 'Digit2') action = h.focusEditor

      if (!action) return
      // 열린 대화상자 안에서는 단축키가 뒤의 화면을 건드리지 않는다.
      if (document.querySelector('dialog[open]')) return
      event.preventDefault()
      event.stopPropagation()
      action()
    }
    window.addEventListener('keydown', onKeyDown, { capture: true })
    return () => window.removeEventListener('keydown', onKeyDown, { capture: true })
  }, [])
}
