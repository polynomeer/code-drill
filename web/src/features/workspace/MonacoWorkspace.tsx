import Editor from '@monaco-editor/react'
import type { OnMount } from '@monaco-editor/react'
import { setupMonaco } from './monacoSetup'
import { useEditorTheme } from './editorSettings'
import type { EditorSettings } from './editorSettings'
import { monacoThemeName } from './editorThemes'

// 모듈이 로드되는 시점 = 에디터가 실제로 필요해진 시점이다.
setupMonaco()

export type EditorHandle = { focus: () => void; revealLine: (line: number, column?: number) => void }

export default function MonacoWorkspace({
  source,
  language,
  onChange,
  settings,
  onReady,
  label = '코드 편집기',
  readOnly = false,
}: {
  source: string
  language: string
  onChange: (next: string) => void
  /** 없으면 예전 기본값 — 프로젝트형 작업 공간은 아직 설정을 따르지 않는다 */
  settings?: EditorSettings
  /** 포커스·줄 이동을 바깥에서 부를 수 있게 손잡이를 건넨다 (단축키 ⌥2, 컴파일 오류 줄) */
  onReady?: (handle: EditorHandle) => void
  label?: string
  /** 제출한 코드처럼 고칠 수 없는 것을 보일 때 */
  readOnly?: boolean
}) {
  const theme = useEditorTheme()
  const handleMount: OnMount = (editor) => {
    onReady?.({
      focus: () => editor.focus(),
      revealLine: (line, column = 1) => {
        editor.revealLineInCenter(line)
        editor.setPosition({ lineNumber: line, column })
        editor.focus()
      },
    })
  }

  return (
    <Editor
      language={language}
      // 기본은 앱 테마를 따르는 픽셀 테마, 설정에서 바꾼다 (docs/ui-overhaul.md §2 — 에디터 테마)
      theme={monacoThemeName(theme)}
      value={source}
      onChange={(next) => onChange(next ?? '')}
      onMount={handleMount}
      options={{
        minimap: { enabled: settings?.minimap ?? false },
        fontSize: settings?.fontSize ?? 13,
        tabSize: settings?.tabSize ?? 4,
        wordWrap: settings?.wordWrap ? 'on' : 'off',
        fontFamily: "'JetBrains Mono Variable', 'JetBrains Mono', ui-monospace, Menlo, monospace",
        fontLigatures: false,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        ariaLabel: label,
        readOnly,
        domReadOnly: readOnly,
        padding: { top: 8 },
      }}
    />
  )
}
