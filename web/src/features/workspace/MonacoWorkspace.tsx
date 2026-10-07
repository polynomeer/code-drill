import Editor from '@monaco-editor/react'
import type { OnMount } from '@monaco-editor/react'
import { useEffect, useRef } from 'react'
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
  const editorRef = useRef<Parameters<OnMount>[0] | null>(null)
  // 편집기가 내보냈지만 아직 [source] 로 돌아오지 않은 값들, 오래된 것부터. 느린 기기에서는 부모가 이 중
  // 앞선 값으로 늦게 다시 그린다 — 그것은 바깥에서 온 값이 아니다.
  const inFlight = useRef<string[]>([])
  const applying = useRef(false)
  // 마운트 콜백은 처음 그린 때의 것이 불릴 수 있다 — 그 사이에 초안이 왔으면 가장 새 값을 넣어야 한다
  const latest = useRef(source)
  latest.current = source

  /**
   * 바깥에서 온 값(초안 불러오기·언어 전환)만 편집기에 넣는다. 편집기를 `value` 로 제어하면 친 글자가 상태를
   * 한 바퀴 돌아 늦게 돌아오고, 느린 기기에서는 그 사이에 친 글자를 지난 값이 덮어써 잃는다 — CI 에서
   * `# typed before sign-in` 이 `#typed befor ign-in` 이 됐다.
   */
  const apply = (editor: Parameters<OnMount>[0], next: string) => {
    inFlight.current = []
    if (editor.getValue() === next) return
    applying.current = true
    const model = editor.getModel()
    if (readOnly || !model) editor.setValue(next)
    else {
      // 실행 취소로 되돌릴 수 있게 편집으로 넣는다
      editor.executeEdits('external', [{ range: model.getFullModelRange(), text: next, forceMoveMarkers: true }])
      editor.pushUndoStop()
    }
    applying.current = false
  }

  useEffect(() => {
    const editor = editorRef.current
    if (!editor) return
    const echoed = inFlight.current.indexOf(source)
    if (echoed >= 0) inFlight.current.splice(0, echoed + 1)
    else if (source !== editor.getValue()) apply(editor, source)
  })

  const handleMount: OnMount = (editor) => {
    editorRef.current = editor
    apply(editor, latest.current)
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
      defaultValue={source}
      onChange={(next) => {
        if (applying.current) return
        // 부모가 값을 바꿔 돌려주면 영영 맞지 않으므로 길이를 묶는다
        inFlight.current = [...inFlight.current.slice(-49), next ?? '']
        onChange(next ?? '')
      }}
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
