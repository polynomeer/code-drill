import { useCallback, useEffect, useState } from 'react'
import { createSubmission, getDraft, getProblem, listSubmissions, logout } from './api/client'
import { getSession, onSessionChange, type Session } from './api/session'
import { AccountSettings } from './features/auth/AccountSettings'
import { SignIn } from './features/auth/SignIn'
import { ProblemList } from './features/problems/ProblemList'
import { ReplayView } from './features/replay/ReplayView'
import { HistoryPanel } from './features/submissions/HistoryPanel'
import { useSubmissionEvents } from './features/submissions/useSubmissionEvents'
import { CoachingPanel } from './features/coaching/CoachingPanel'
import { CompetencyMapPanel } from './features/competency/CompetencyMapPanel'
import { CodeView } from './features/submissions/CodeView'
import { VerdictPanel } from './features/submissions/VerdictPanel'
import { PreQuestionPanel } from './features/workspace/PreQuestionPanel'
import { TestPanel } from './features/workspace/TestPanel'
import { Workspace } from './features/workspace/Workspace'
import { starterFor } from './features/workspace/starters'
import { setParam } from './shared/url'
import { useAutoSave } from './features/workspace/useAutoSave'
import type { Problem, Submission, SubmissionLanguage } from './shared/types'

/**
 * 제품 코어 화면 (기술 설계서 §16.1 P3, 디자인 설계서 §2.1).
 *
 * 문제 탐색 → 코드 작성(자동 저장) → 제출 → SSE → 판정 → 리플레이 → 기록.
 * Judge first 원칙에 따라 판정 결과가 항상 먼저 보이고, 리플레이와 기록은 그 아래에 붙는다.
 */
export function App() {
  // 세션이 없으면 아무것도 그리지 않는다. 제출·초안·기록이 전부 인증을 요구하므로,
  // 로그인 전 화면은 실패한 요청 목록이 될 뿐이다.
  const [session, setSession] = useState<Session | null>(getSession)

  // 토큰 갱신이 끝내 실패하면 세션 모듈이 스스로 비운다. 그때 화면도 로그인으로
  // 돌아가야 한다 — 그러지 않으면 사용자는 아무 반응 없는 화면을 보게 된다.
  useEffect(() => onSessionChange(setSession), [])

  if (!session) return <SignIn onSignedIn={setSession} />
  return <Drill session={session} />
}

function Drill({ session }: { session: Session }) {
  const [settingsOpen, setSettingsOpen] = useState(false)
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
  // 코드를 펼쳐 본 제출. 판정을 여는 것과 다른 상태다 — 기록을 보다가 코드만 확인하고
  // 돌아오는 흐름이 흔하다.
  const [viewingCode, setViewingCode] = useState<string | null>(null)

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
    setParam('submission', id)
  }

  const selectProblem = (next: string) => {
    setSlug(next)
    setTouched(false)
    setSubmissionId(null)
    // 필터는 지우지 않는다. 문제를 바꿨다고 사용자가 걸어 둔 조건까지 풀 이유가 없다.
    setParam('submission', null)
  }

  const replayInput = numberArray(problem?.samples[0]?.args[0])
  const viewed = history.find((item) => item.id === viewingCode) ?? null

  return (
    <div className="app">
      <header className="top">
        <strong>CodeDrill</strong>
        <span className="muted">문제를 푸는 것이 아니라, 문제 해결 역량을 훈련합니다</span>
        <span className="who">
          <button
            type="button"
            className="linklike"
            onClick={() => setSettingsOpen((open) => !open)}
            aria-expanded={settingsOpen}
          >
            {session.displayName}
          </button>
          <button type="button" className="linklike" onClick={() => void logout()}>
            로그아웃
          </button>
        </span>
      </header>

      {error && <p className="warn">{error}</p>}

      <main className="columns">
        <div className="stack">
          {settingsOpen && (
            <AccountSettings session={session} onClose={() => setSettingsOpen(false)} />
          )}
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
          {/* 에디터 위에 둔다. "풀기 전에" 묻는 질문이 코드 아래 있으면 이미 늦다. */}
          {problem && <PreQuestionPanel problemId={problem.id} />}
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
          {/* 판정 앞에 둔다. 제출하기 전에 돌려 보는 것이 이 패널의 목적이다. */}
          {problem && <TestPanel problem={problem} language={language} source={source} />}
          {/* 에디터와 테스트 **아래**에 둔다. 위에 두면 막히기 전에 눈에 들어오고,
              그러면 관문이 아니라던 말과 어긋난다 (FR-802). */}
          {problem && <CoachingPanel problemId={problem.id} onOpenProblem={selectProblem} />}
          {submission && <VerdictPanel submission={submission} />}
          {trace && submissionId && (
            <ReplayView submissionId={submissionId} manifest={trace} input={replayInput} />
          )}
          {viewed && (
            <CodeView
              submission={viewed}
              previous={previousOf(history, viewed)}
              onClose={() => setViewingCode(null)}
            />
          )}
          {/* 기록 아래에 둔다. "무엇을 풀었나" 다음에 "그래서 무엇이 늘었나"가 온다. */}
          <CompetencyMapPanel onOpenSubmission={openSubmission} />
          <HistoryPanel
            submissions={history}
            currentId={submissionId}
            onOpen={openSubmission}
            onView={setViewingCode}
          />
        </div>
      </main>
    </div>
  )
}

/**
 * 기록에서 바로 다음(= 시간상 이전) 제출.
 *
 * 목록은 최신순이므로 뒤에 있는 것이 이전 제출이다. 첫 제출이면 비교 대상이 없다.
 */
function previousOf(history: Submission[], current: Submission): Submission | null {
  const index = history.findIndex((item) => item.id === current.id)
  return index >= 0 ? (history[index + 1] ?? null) : null
}

/** `fun twoSum(nums: IntArray, ...): IntArray` → `twoSum` */
function functionName(signature: string): string {
  return signature.slice(signature.indexOf(' ') + 1, signature.indexOf('(')).trim()
}

/**
 * 배열 렌더러가 시작 상태로 삼을 첫 인자.
 *
 * 격자는 행 우선으로 편다. 계측 이벤트의 인덱스가 `행 * 열 + 열` 이므로 편 배열과 자리가
 * 맞는다 — 격자로 그리지는 못해도 어느 칸을 봤는지는 그대로 따라간다.
 */
function numberArray(value: unknown): number[] {
  if (!Array.isArray(value)) return []
  return value.flatMap((item) =>
    Array.isArray(item)
      ? item.filter((cell): cell is number => typeof cell === 'number')
      : typeof item === 'number'
        ? [item]
        : [],
  )
}
