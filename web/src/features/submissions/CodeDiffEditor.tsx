import { DiffEditor } from '@monaco-editor/react'
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
  return (
    <DiffEditor
      original={original}
      modified={modified}
      language={language}
      theme="vs-dark"
      options={{
        readOnly: true,
        originalEditable: false,
        renderSideBySide: sideBySide,
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
