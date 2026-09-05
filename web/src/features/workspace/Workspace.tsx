import { Suspense, lazy } from 'react'

/**
 * 코칭 Workspace (디자인 설계서 §2.1).
 *
 * Monaco 는 무겁다. 첫 화면은 문제와 판정을 먼저 보여줘야 하므로(§0.1 빠름) 에디터를
 * 별도 청크로 미루고, 그 사이에도 문제를 읽을 수 있게 한다.
 *
 * 슬라이스는 에디터와 제출만 담는다. 자동 저장(CAS), 커스텀 테스트, 코칭 질문은 다음
 * 단계다.
 */
const MonacoWorkspace = lazy(() => import('./MonacoWorkspace'))

export function Workspace({
  source,
  onChange,
  onSubmit,
  submitting,
}: {
  source: string
  onChange: (next: string) => void
  onSubmit: () => void
  submitting: boolean
}) {
  return (
    <section className="panel editor-panel">
      <div className="editor-header">
        <h3>풀이</h3>
        <button className="primary" onClick={onSubmit} disabled={submitting}>
          {submitting ? '제출 중…' : '제출'}
        </button>
      </div>
      <div className="editor">
        <Suspense fallback={<p className="muted editor-loading">에디터를 불러오는 중…</p>}>
          <MonacoWorkspace source={source} onChange={onChange} />
        </Suspense>
      </div>
    </section>
  )
}
