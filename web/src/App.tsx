import { useEffect, useState } from 'react'
import { createSubmission, getProblem, listProblems } from './api/client'
import { ReplayView } from './features/replay/ReplayView'
import { useSubmissionEvents } from './features/submissions/useSubmissionEvents'
import { VerdictPanel } from './features/submissions/VerdictPanel'
import { Workspace } from './features/workspace/Workspace'
import { starterFor } from './features/workspace/starters'
import type { Problem, ProblemSummary, SubmissionLanguage } from './shared/types'

/**
 * 첫 화면 (기술 설계서 §16.2, 디자인 설계서 §2.1).
 *
 * 문제 열기 → 코드 작성 → 제출 → SSE → 판정 → 리플레이. Judge first 원칙에 따라 판정
 * 결과가 항상 먼저 보이고, 리플레이는 그 아래에 붙는다 (디자인 설계서 §0.2).
 */
export function App() {
  const [problems, setProblems] = useState<ProblemSummary[]>([])
  const [slug, setSlug] = useState<string | null>(null)
  const [problem, setProblem] = useState<Problem | null>(null)
  const [language, setLanguage] = useState<SubmissionLanguage>('KOTLIN')
  const [source, setSource] = useState('')
  // ?submission=<id> 로 특정 제출을 바로 연다. 결과를 공유하거나 다시 열어보는 경로이며,
  // 새로고침해도 방금 본 판정이 사라지지 않는다.
  const [submissionId, setSubmissionId] = useState<string | null>(
    () => new URLSearchParams(window.location.search).get('submission'),
  )
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { submission, trace } = useSubmissionEvents(submissionId)

  useEffect(() => {
    listProblems()
      .then((list) => {
        setProblems(list)
        setSlug((current) => current ?? list[0]?.id ?? null)
      })
      .catch((e: Error) => setError(e.message))
  }, [])

  useEffect(() => {
    if (!slug) return
    getProblem(slug).then(setProblem).catch((e: Error) => setError(e.message))
  }, [slug])

  // 문제나 언어가 바뀌면 시작 코드를 갈아 끼운다. 사용자가 고친 코드를 덮어쓰지
  // 않도록, 아직 손대지 않았을 때만 바꾼다.
  const [touched, setTouched] = useState(false)
  useEffect(() => {
    if (!problem || touched) return
    setSource(starterFor(language, problem.signature, functionName(problem.signature)))
  }, [problem, language, touched])

  const submit = async () => {
    if (!problem) return
    setSubmitting(true)
    setError(null)
    try {
      const created = await createSubmission(problem.id, problem.version, language, source)
      setSubmissionId(created.id)
      window.history.replaceState(null, '', `?submission=${created.id}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const replayInput = numberArray(problem?.samples[0]?.args[0])

  return (
    <div className="app">
      <header className="top">
        <strong>CodeDrill</strong>
        <span className="muted">문제를 푸는 것이 아니라, 문제 해결 역량을 훈련합니다</span>
        <select
          className="problem-picker"
          value={slug ?? ''}
          onChange={(event) => {
            setSlug(event.target.value)
            setTouched(false)
            setSubmissionId(null)
            window.history.replaceState(null, '', window.location.pathname)
          }}
          aria-label="문제 선택"
        >
          {problems.map((item) => (
            <option key={item.id} value={item.id}>
              {item.title}
            </option>
          ))}
        </select>
      </header>

      {error && <p className="warn">{error}</p>}

      <main className="columns">
        <section className="panel statement">
          <h3>{problem?.title ?? '불러오는 중…'}</h3>
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

        <div className="stack">
          <Workspace
            source={source}
            language={language}
            onChange={(next) => {
              setTouched(true)
              setSource(next)
            }}
            onLanguageChange={(next) => {
              setLanguage(next)
              setTouched(false)
            }}
            onSubmit={submit}
            submitting={submitting}
          />
          {submission && <VerdictPanel submission={submission} />}
          {trace && <ReplayView capture={trace} input={replayInput} />}
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
