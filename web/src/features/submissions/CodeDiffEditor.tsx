import { DiffEditor } from '@monaco-editor/react'
import { useEditorTheme } from '../workspace/editorSettings'
import { monacoThemeName } from '../workspace/editorThemes'
import { setupMonaco } from '../workspace/monacoSetup'

setupMonaco()

/**
 * 두 제출의 코드 비교 (디자인 설계서 §9.3 — side-by-side 기본, 좁은 화면은 한 줄로).
 *
 * 따로 청크로 온다 — 비교 화면을 열 때만 Monaco 가 필요하다.
 */
export default function CodeDiffEditor({
  original,
  modified,
  language,
  sideBySide,
}: {
  original: string
  modified: string
  language: string
  sideBySide: boolean
}) {
  const theme = useEditorTheme()
  return (
    <DiffEditor
      original={original}
      modified={modified}
      language={language}
      theme={monacoThemeName(theme)}
      options={{
        readOnly: true,
        originalEditable: false,
        renderSideBySide: sideBySide,
        // Monaco 는 폭이 900px 아래면 스스로 한 줄 보기로 바꾼다. 부른 쪽이 나란히를 골랐으면 그대로 둔다 —
        // 유사도 검수는 두 소스를 나란히 보는 것이 일이다
        useInlineViewWhenSpaceIsLimited: !sideBySide,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        fontFamily: "'JetBrains Mono Variable', 'JetBrains Mono', ui-monospace, Menlo, monospace",
        fontSize: 13,
        fontLigatures: false,
      }}
    />
  )
}
