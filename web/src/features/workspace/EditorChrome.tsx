import { useEffect } from 'react'
import { useEditorTheme } from './editorSettings'

/**
 * 에디터 둘레의 색을 고른 에디터 테마에 맞춘다.
 *
 * 풀이 화면의 에디터 틀, 리플레이의 코드 창, 비교·검수 화면은 `--color-bg-editor`·`--color-text-on-editor` 로
 * 칠한다. 토큰은 앱 테마별 값만 아는데, 에디터는 앱과 다른 테마를 고를 수 있다 — 그대로 두면 밝은 에디터를
 * 어두운 틀이 두른다. 그래서 고른 테마의 바탕과 보조 글자를 `<html>` 에 덮어쓴다. 그리는 것은 없다.
 */
export function EditorChrome() {
  const theme = useEditorTheme()
  useEffect(() => {
    const root = document.documentElement.style
    root.setProperty('--color-bg-editor', theme.colors.background)
    root.setProperty('--color-text-on-editor', theme.colors.muted)
    // 리플레이 코드 창의 현재 줄 — 에디터의 현재 줄과 같은 색, 그 위 글자는 본문색 (둘은 대비 시험을 거친 쌍이다)
    root.setProperty('--color-editor-line', theme.colors.lineHighlight)
    root.setProperty('--color-editor-text', theme.colors.foreground)
  }, [theme])
  return null
}
