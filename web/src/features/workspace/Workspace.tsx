import { Suspense, lazy } from 'react'
import { EDITOR_LANGUAGE, LANGUAGE_LABEL } from '../../shared/types'
import type { SubmissionLanguage } from '../../shared/types'

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
  language,
  onChange,
  onLanguageChange,
  onSubmit,
  submitting,
}: {
  source: string
  language: SubmissionLanguage
  onChange: (next: string) => void
  onLanguageChange: (next: SubmissionLanguage) => void
  onSubmit: () => void
  submitting: boolean
}) {
  return (
    <section className="panel editor-panel">
      <div className="editor-header">
        <h3>풀이</h3>
        <div className="editor-actions">
          <label className="muted" htmlFor="language">
            언어
          </label>
          <select
            id="language"
            value={language}
            onChange={(event) => onLanguageChange(event.target.value as SubmissionLanguage)}
          >
            {(Object.keys(LANGUAGE_LABEL) as SubmissionLanguage[]).map((value) => (
              <option key={value} value={value}>
                {LANGUAGE_LABEL[value]}
              </option>
            ))}
          </select>
          <button className="primary" onClick={onSubmit} disabled={submitting}>
            {submitting ? '제출 중…' : '제출'}
          </button>
        </div>
      </div>
      <div className="editor">
        <Suspense fallback={<p className="muted editor-loading">에디터를 불러오는 중…</p>}>
          <MonacoWorkspace
            source={source}
            language={EDITOR_LANGUAGE[language]}
            onChange={onChange}
          />
        </Suspense>
      </div>
    </section>
  )
}
