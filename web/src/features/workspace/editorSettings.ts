import { useSyncExternalStore } from 'react'

/**
 * 에디터 설정 (디자인 설계서 §11.2 Editor — 글꼴 크기, tab size, minimap, 줄바꿈).
 *
 * 기기마다 화면이 달라 글꼴 크기 같은 값은 기기별이 자연스럽다. 그래서 지금은 localStorage
 * 에 둔다. 계정에 묶는 것은 설정 화면이 서버 설정을 갖출 때 함께 한다 (ui-overhaul.md §6.9).
 *
 * keymap(Vim·Emacs)은 아직 없다. monaco-vim 이 `monaco-editor` 진입점을 통째로 import 해서,
 * 그대로 들이면 번들 예산이 막아 둔 9MB 워커가 돌아온다.
 */
export type EditorSettings = {
  fontSize: number
  tabSize: 2 | 4
  minimap: boolean
  wordWrap: boolean
}

export const DEFAULT_EDITOR_SETTINGS: EditorSettings = {
  fontSize: 14,
  tabSize: 4,
  minimap: false,
  wordWrap: false,
}

export const FONT_SIZES = [12, 13, 14, 15, 16, 18, 20] as const

const KEY = 'codedrill.editor'
const listeners = new Set<() => void>()
let current = read()

function read(): EditorSettings {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return DEFAULT_EDITOR_SETTINGS
    const parsed = JSON.parse(raw) as Partial<EditorSettings>
    // 저장된 값이 옛 모양이어도 빠진 칸은 기본값으로 채운다.
    return { ...DEFAULT_EDITOR_SETTINGS, ...parsed }
  } catch {
    return DEFAULT_EDITOR_SETTINGS
  }
}

export function updateEditorSettings(patch: Partial<EditorSettings>) {
  current = { ...current, ...patch }
  try {
    localStorage.setItem(KEY, JSON.stringify(current))
  } catch {
    // 저장하지 못해도 이번 탭에는 적용한다.
  }
  listeners.forEach((listener) => listener())
}

export function useEditorSettings(): EditorSettings {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    () => current,
  )
}
