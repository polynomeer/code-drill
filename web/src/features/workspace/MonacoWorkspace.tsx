import Editor from '@monaco-editor/react'
import { setupMonaco } from './monacoSetup'

// 모듈이 로드되는 시점 = 에디터가 실제로 필요해진 시점이다.
setupMonaco()

export default function MonacoWorkspace({
  source,
  language,
  onChange,
}: {
  source: string
  language: string
  onChange: (next: string) => void
}) {
  return (
    <Editor
      language={language}
      theme="vs-dark"
      value={source}
      onChange={(next) => onChange(next ?? '')}
      options={{
        minimap: { enabled: false },
        fontSize: 13,
        scrollBeyondLastLine: false,
        automaticLayout: true,
      }}
    />
  )
}
