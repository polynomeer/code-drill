import { useCallback, useEffect, useState } from 'react'
import { createSubmission, getDraft, getProblem, listSubmissions } from './api/client'
import { ProblemList } from './features/problems/ProblemList'
import { ReplayView } from './features/replay/ReplayView'
import { HistoryPanel } from './features/submissions/HistoryPanel'
import { useSubmissionEvents } from './features/submissions/useSubmissionEvents'
import { VerdictPanel } from './features/submissions/VerdictPanel'
import { Workspace } from './features/workspace/Workspace'
import { starterFor } from './features/workspace/starters'
import { useAutoSave } from './features/workspace/useAutoSave'
import type { Problem, Submission, SubmissionLanguage } from './shared/types'

/**
 * 제품 코어 화면 (기술 설계서 §16.1 P3, 디자인 설계서 §2.1).
 *
 * 문제 탐색 → 코드 작성(자동 저장) → 제출 → SSE → 판정 → 리플레이 → 기록.
 * Judge first 원칙에 따라 판정 결과가 항상 먼저 보이고, 리플레이와 기록은 그 아래에 붙는다.
 */
export function App() {
  const [slug, setSlug] = useState<string | null>(null)
  const [problem, setProblem] = useState<Problem | null>(null)
  const [language, setLanguage] = useState<SubmissionLanguage>('KOTLIN')
  const [source, setSource] = useState('')
  const [touched, setTouched] = useState(false)
  const [history, setHistory] = useState<Submission[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // ?submission=<id> 로 특정 제출을 바로 연다. 결과를 공유하거나 다시 열어보는 경로이며,
  // 새로고침해도 방금 본 판정이 사라지지 않는다.
  const [submissionId, setSubmissionId] = useState<string | null>(
    () => new URLSearchParams(window.location.search).get('submission'),
  )

  const { submission, trace } = useSubmissionEvents(submissionId)
  const autoSave = useAutoSave(slug, language, source, touched)

  useEffect(() => {
    if (!slug) return
    getProblem(slug).then(setProblem).catch((e: Error) => setError(e.message))
  }, [slug])

  // ?submission=<id> 로 바로 들어오면 고른 문제가 없다. 제출이 어떤 문제였는지 알고
  // 있으므로 그것으로 채운다 — 문제 없이는 리플레이가 배열을 그릴 입력조차 없다.
  useEffect(() => {
    if (submission && !slug) setSlug(submission.problemId)
  }, [submission, slug])

  const refreshHistory = useCallback(() => {
    if (!slug) return
    listSubmissions(slug)
      .then((page) => setHistory(page.items))
      .catch(() => {
        // 기록을 못 읽어도 풀이는 계속돼야 한다.
      })
  }, [slug])

  useEffect(refreshHistory, [refreshHistory])

  // 판정이 끝나면 기록을 새로 읽는다.
  useEffect(() => {
    if (submission?.status === 'COMPLETED') refreshHistory()
  }, [submission?.status, refreshHistory])

  /**
   * 편집기에 채울 코드를 정한다.
   *
   * 저장된 초안이 있으면 그것이 먼저다 (§0.1 복구 가능). 없으면 언어별 시작 코드를 준다.
   * 사용자가 이미 손댄 뒤에는 어느 쪽도 덮어쓰지 않는다.
   */
  useEffect(() => {
    if (!problem || touched) return
    let cancelled = false
    getDraft(problem.id, language)
      .then((draft) => {
        if (cancelled) return
        setSource(
          draft?.code ?? starterFor(language, problem.signature, functionName(problem.signature)),
        )
      })
      .catch(() => {
        if (!cancelled) {
          setSource(starterFor(language, problem.signature, functionName(problem.signature)))
        }
      })
    return () => {
      cancelled = true
    }
  }, [problem, language, touched])

  const submit = async () => {
    if (!problem) return
    setSubmitting(true)
    setError(null)
    try {
      const created = await createSubmission(problem.id, problem.version, language, source)
      openSubmission(created.id)
      refreshHistory()
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const openSubmission = (id: string) => {
    setSubmissionId(id)
    window.history.replaceState(null, '', `?submission=${id}`)
  }

  const selectProblem = (next: string) => {
    setSlug(next)
    setTouched(false)
    setSubmissionId(null)
    window.history.replaceState(null, '', window.location.pathname)
  }

  const replayInput = numberArray(problem?.samples[0]?.args[0])

  return (
    <div className="app">
      <header className="top">
        <strong>CodeDrill</strong>
        <span className="muted">문제를 푸는 것이 아니라, 문제 해결 역량을 훈련합니다</span>
      </header>

      {error && <p className="warn">{error}</p>}

      <main className="columns">
        <div className="stack">
          <ProblemList selected={slug} onSelect={selectProblem} />
          <section className="panel statement">
            <h3>{problem?.title ?? '문제를 고르세요'}</h3>
            {problem && (
              <>
                <p className="muted">
                  {problem.timeMillis}ms · {problem.memoryMb}MB · {problem.signature}
                </p>
                <ul className="group-weights">
                  {problem.groups.map((group) => (
                    <li key={group.id}>
                      <span>{group.id}</span>
                      <span className="muted">
                        {group.weight}점 ·{' '}
                        {group.aggregation === 'SUM' ? '부분 점수' : '전부 통과해야 만점'} ·{' '}
                        {group.caseCount}케이스
                      </span>
                    </li>
                  ))}
                </ul>
                <pre className="statement-body">{problem.statement}</pre>
              </>
            )}
          </section>
        </div>

        <div className="stack">
          <Workspace
            source={source}
            language={language}
            saveState={autoSave.state}
            onResolveConflict={(code, version) => {
              if (code !== null) {
                // 서버 것 가져오기: 편집기를 서버 내용으로 맞추고 저장은 하지 않는다.
                setSource(code)
                autoSave.resolveConflict(version)
              } else {
                // 내 것 유지: 편집 중인 내용을 그대로 덮어쓴다.
                autoSave.resolveConflict(version, source)
              }
            }}
            onChange={(next) => {
              setTouched(true)
              setSource(next)
            }}
            onLanguageChange={(next) => {
              setLanguage(next)
              setTouched(false)
            }}
            onSubmit={submit}
            submitting={submitting || !problem}
          />
          {submission && <VerdictPanel submission={submission} />}
          {trace && submissionId && (
            <ReplayView submissionId={submissionId} manifest={trace} input={replayInput} />
          )}
          <HistoryPanel
            submissions={history}
            currentId={submissionId}
            onOpen={openSubmission}
          />
        </div>
      </main>
    </div>
  )
}

/** `fun twoSum(nums: IntArray, ...): IntArray` → `twoSum` */
function functionName(signature: string): string {
  return signature.slice(signature.indexOf(' ') + 1, signature.indexOf('(')).trim()
}

function numberArray(value: unknown): number[] {
  return Array.isArray(value) ? value.filter((item): item is number => typeof item === 'number') : []
}
