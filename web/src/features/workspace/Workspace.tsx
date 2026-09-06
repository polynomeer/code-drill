import { Suspense, lazy } from 'react'
import { EDITOR_LANGUAGE, LANGUAGE_LABEL } from '../../shared/types'
import type { SubmissionLanguage } from '../../shared/types'
import type { SaveState } from './useAutoSave'

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
  saveState,
  onChange,
  onLanguageChange,
  onResolveConflict,
  onSubmit,
  submitting,
}: {
  source: string
  language: SubmissionLanguage
  saveState: SaveState
  onChange: (next: string) => void
  onLanguageChange: (next: SubmissionLanguage) => void
  /** code 가 null 이면 편집 중인 내용을 유지하고 그대로 덮어쓴다. */
  onResolveConflict: (code: string | null, version: number) => void
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
      <SaveIndicator state={saveState} onResolve={onResolveConflict} />
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

/**
 * 자동 저장 상태.
 *
 * 충돌은 배너로 남긴다. 토스트처럼 사라지면 사용자가 선택하기 전에 놓친다 —
 * 그 순간 잃는 것이 작성 중이던 코드다.
 */
function SaveIndicator({
  state,
  onResolve,
}: {
  state: SaveState
  onResolve: (code: string | null, version: number) => void
}) {
  if (state.status === 'conflict') {
    return (
      <div className="conflict">
        <p>
          다른 곳에서 이 초안이 저장됐습니다. 어느 쪽을 남길지 고르세요.
          <span className="muted"> (서버 버전 {state.current.version})</span>
        </p>
        <div className="conflict-actions">
          <button onClick={() => onResolve(state.current.code, state.current.version)}>
            서버 것 가져오기
          </button>
          <button className="primary" onClick={() => onResolve(null, state.current.version)}>
            내 것 유지하고 덮어쓰기
          </button>
        </div>
      </div>
    )
  }

  const label = {
    idle: '',
    saving: '저장 중…',
    saved: '저장됨',
    failed: state.status === 'failed' ? `저장 실패: ${state.message}` : '',
  }[state.status]

  return <p className={`save-state ${state.status === 'failed' ? 'warn' : 'muted'}`}>{label}</p>
}
