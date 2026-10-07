import { describe, expect, it } from 'vitest'
import { EDITOR_THEMES, isEditorThemeChoice, monacoThemeName, resolveEditorTheme } from './editorThemes'

function luminance(hex: string) {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r! + 0.7152 * g! + 0.0722 * b!
}

function ratio(a: string, b: string) {
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p)
  return (x! + 0.05) / (y! + 0.05)
}

describe('에디터 테마 대비', () => {
  // 우리가 정의한 테마만 잰다. Monaco 내장 테마의 색은 우리가 고칠 수 없다
  for (const theme of EDITOR_THEMES.filter((item) => item.custom)) {
    it(`${theme.label}: 코드 글자는 4.5:1, 줄 번호는 3:1`, () => {
      const { background, lineNumber, lineHighlight, ...rest } = theme.colors
      for (const key of ['foreground', 'muted', 'comment', 'keyword', 'string', 'number', 'type'] as const) {
        expect(ratio(rest[key], background), `${key}`).toBeGreaterThanOrEqual(4.5)
        // 현재 줄 강조 위에서도 읽혀야 한다
        expect(ratio(rest[key], lineHighlight), `${key} on line highlight`).toBeGreaterThanOrEqual(4.5)
      }
      expect(ratio(lineNumber, background)).toBeGreaterThanOrEqual(3)
      expect(ratio(rest.activeLineNumber, background)).toBeGreaterThanOrEqual(3)
      expect(ratio(rest.cursor, background)).toBeGreaterThanOrEqual(3)
    })
  }
})

describe('resolveEditorTheme', () => {
  it('auto 는 앱 테마를 따라 픽셀 테마를 고른다', () => {
    expect(resolveEditorTheme('auto', 'dark').id).toBe('pixel-dark')
    expect(resolveEditorTheme('auto', 'light').id).toBe('pixel-light')
  })

  it('고른 테마는 앱 테마와 무관하다', () => {
    expect(resolveEditorTheme('forest', 'light').id).toBe('forest')
  })

  it('저장된 값이 알 수 없는 것이면 받지 않는다', () => {
    expect(isEditorThemeChoice('monokai')).toBe(false)
    expect(isEditorThemeChoice('paper')).toBe(true)
  })

  it('우리 테마는 접두를 붙여 Monaco 내장 이름과 겹치지 않는다', () => {
    expect(monacoThemeName(resolveEditorTheme('paper', 'light'))).toBe('codedrill-paper')
    expect(monacoThemeName(resolveEditorTheme('vs', 'light'))).toBe('vs')
  })
})
