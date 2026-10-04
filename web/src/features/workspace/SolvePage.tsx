import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, GraduationCap, LogIn, PanelLeftClose, PanelLeftOpen, Play, Send, Settings, Swords } from 'lucide-react'
import { Suspense, lazy, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Group, Panel, Separator, useDefaultLayout, usePanelRef } from 'react-resizable-panels'
import { Link, useLocation } from 'wouter'
import { createSubmission, getOnboarding, getProblem, getSubmissionSource, listSubmissions } from '../../api/client'
import { useSession } from '../../api/session'
import {
  Button,
  EmptyState,
  IconButton,
  InlineAlert,
  JudgeStatusBadge,
  Select,
  Skeleton,
  Tabs,
  VerdictBadge,
  useToast,
} from '../../design'
import { IN_FLIGHT, LANGUAGE_LABEL, EDITOR_LANGUAGE } from '../../shared/types'
import type { Submission, SubmissionLanguage } from '../../shared/types'
import { setParam } from '../../shared/url'
import { useMediaQuery } from '../../shared/useMediaQuery'
import { CoachingPanel } from '../coaching/CoachingPanel'
import { DuelDialog } from '../contest/DuelDialog'
import { DiscussionPanel } from '../discussion/DiscussionPanel'
import { EditorialPanel } from '../lab/EditorialPanel'
import { ReplayView } from '../replay/ReplayView'
import { sampleArray } from '../replay/timeline'
import { CodeView } from '../submissions/CodeView'
import { HistoryPanel } from '../submissions/HistoryPanel'
import { useSubmissionEvents } from '../submissions/useSubmissionEvents'
import { VerdictPanel } from '../submissions/VerdictPanel'
import { EditorSettingsDialog } from './EditorSettingsDialog'
import type { EditorHandle } from './MonacoWorkspace'
import { PreQuestionPanel } from './PreQuestionPanel'
import { Markdown, StatementView } from './StatementView'
import { TestPanel } from './TestPanel'
import type { TestPanelHandle } from './TestPanel'
import { SaveIndicator } from './SaveIndicator'
import { useEditorSettings } from './editorSettings'
import { splitStatement } from './statement'
import { ALT, MOD, useShortcuts } from './useShortcuts'
import { useWorkspaceSource } from './useWorkspaceSource'
import styles from './SolvePage.module.css'
import { takeOpenSource, track, trackOnce } from '../../shared/analytics'

/**
 * S-01 풀이 Workspace (UI 디자인 문서 §4, 디자인 설계서 §6 — docs/ui-overhaul.md §6.2).
 *
 * 문제 38% / 에디터 62% 분할에 하단 결과 창. 예전에는 지문과 에디터가 한 페이지의 두 열에
 * 3,700px 떨어져 있어 지문을 읽으며 코드를 쓸 수 없었다.
 *
 * 주소가 상태다: `/problems/:slug/solve?lang=&submission=&step=`. 새로고침하거나 링크로
 * 건네도 같은 문제·언어·제출·리플레이 걸음에서 다시 선다 (디자인 설계서 §2.3).
 */
const MonacoWorkspace = lazy(() => import('./MonacoWorkspace'))

type ProblemTab = 'statement' | 'editorial' | 'discussion' | 'submissions'
type DrawerTab = 'tests' | 'verdict' | 'replay'
type MobileTab = 'problem' | 'code' | 'result'

const LANGUAGES = Object.keys(LANGUAGE_LABEL) as SubmissionLanguage[]

function readQuery() {
  const params = new URLSearchParams(window.location.search)
  const lang = params.get('lang')
  const step = params.get('step')
  return {
    language: (LANGUAGES as string[]).includes(lang ?? '') ? (lang as SubmissionLanguage) : 'KOTLIN',
    languageGiven: (LANGUAGES as string[]).includes(lang ?? ''),
    submission: params.get('submission'),
    /** `?from=<제출 id>` — 그 제출의 코드를 편집기로 옮겨 시작한다 (제출 상세의 "이 코드로 편집기 열기") */
    from: params.get('from'),
    step: step === null || Number.isNaN(Number(step)) ? null : Number(step),
  }
}

export function SolvePage({ slug }: { slug: string }) {
  const [, navigate] = useLocation()
  const queryClient = useQueryClient()
  const initial = useMemo(readQuery, [])

  const problemQuery = useQuery({ queryKey: ['problem', slug], queryFn: () => getProblem(slug) })
  const problem = problemQuery.data ?? null

  // 로그인하지 않아도 풀이 화면은 열린다 (디자인 설계서 §11.1). 코드는 이 기기에 남고, 실행·제출할 때
  // 로그인한다 — 돌아오면 쓰던 코드가 이어진다 (useWorkspaceSource).
  const session = useSession()
  const signedIn = session !== null
  const [location] = useLocation()

  const [language, setLanguage] = useState<SubmissionLanguage>(initial.language)
  const workspace = useWorkspaceSource(problem, language, signedIn)
  const settings = useEditorSettings()
  const toast = useToast()

  // 제출한 코드를 새 초안으로 옮긴다. 손댄 것으로 치므로 저장된 초안을 덮지 않고, 자동 저장이 새
  // 초안을 쓴다. 한 번 옮기면 주소에서 지운다 — 새로고침에 다시 덮어쓰면 그 사이 고친 것을 잃는다.
  const { edit } = workspace
  useEffect(() => {
    if (!initial.from || !problem) return
    let cancelled = false
    getSubmissionSource(initial.from)
      .then((code) => {
        if (cancelled) return
        if (code !== null) {
          edit(code)
          toast.show('제출한 코드를 편집기로 옮겼습니다', 'success')
        }
        setParam('from', null)
      })
      .catch(() => toast.show('제출한 코드를 가져오지 못했습니다', 'danger'))
    return () => {
      cancelled = true
    }
    // 문제가 처음 온 순간에 한 번만
  }, [problem?.id])

  // 주소에 언어가 없으면 온보딩에서 고른 주 언어로 시작한다 (docs/ui-overhaul.md §6.9)
  const onboarding = useQuery({ queryKey: ['me', 'onboarding'], queryFn: getOnboarding, enabled: signedIn && !initial.languageGiven })
  const languageChosen = useRef(false)
  useEffect(() => {
    const preferred = onboarding.data?.language as SubmissionLanguage | undefined
    if (!preferred || languageChosen.current || !(LANGUAGES as string[]).includes(preferred)) return
    languageChosen.current = true
    setLanguage(preferred)
  }, [onboarding.data])

  // 로그인 전 초안을 서버 초안으로 이어받았으면 한 번 알린다
  useEffect(() => {
    if (workspace.adopted) toast.show('로그인 전에 쓰던 코드를 이어 씁니다', 'success')
  }, [workspace.adopted])

  const historyQuery = useQuery({
    queryKey: ['submissions', slug],
    queryFn: () => listSubmissions(slug).then((page) => page.items),
    enabled: signedIn,
  })
  const history = historyQuery.data ?? []

  // 열린 제출과 리플레이 걸음 (?submission, ?step — 게시판에 붙은 시점으로도 연다, §8.5)
  const [submissionId, setSubmissionId] = useState<string | null>(initial.submission)
  const [initialStep, setInitialStep] = useState<number | null>(initial.step)
  const [replayStep, setReplayStep] = useState<number | null>(null)
  const { submission, trace } = useSubmissionEvents(submissionId)
  const [viewingCode, setViewingCode] = useState<string | null>(null)

  const [submitting, setSubmitting] = useState(false)
  /** 이 화면에서 낸 제출과 낸 시각 — 판정까지 걸린 시간을 잰다 */
  const submittedAt = useRef(new Map<string, number>())
  const [mountId] = useState(() => Math.random().toString(36).slice(2))
  const [running, setRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [problemTab, setProblemTab] = useState<ProblemTab>('statement')
  const [drawerTab, setDrawerTab] = useState<DrawerTab>(initial.submission ? (initial.step !== null ? 'replay' : 'verdict') : 'tests')
  const [mobileTab, setMobileTab] = useState<MobileTab>(initial.submission ? 'result' : 'problem')
  // 훈련 화면의 "이어 하기"는 `?coaching=1` 로 온다 — 열어 둔 세션을 바로 보이게
  const [resumeCoaching] = useState(() => new URLSearchParams(window.location.search).get('coaching') === '1')
  const [coachingOpen, setCoachingOpen] = useState(resumeCoaching)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [duelOpen, setDuelOpen] = useState(false)

  const testPanel = useRef<TestPanelHandle>(null)
  const editor = useRef<EditorHandle | null>(null)
  const problemPane = useRef<HTMLDivElement>(null)
  const problemPanel = usePanelRef()
  const drawerPanel = usePanelRef()
  const [problemCollapsed, setProblemCollapsed] = useState(false)
  const [drawerCollapsed, setDrawerCollapsed] = useState(false)

  // 중단점 (디자인 설계서 §15.1, UI 디자인 문서 §8.1)
  const isMobile = useMediaQuery('(max-width: 767px)')
  const isTablet = useMediaQuery('(max-width: 1023px)')
  const isNarrowDesktop = useMediaQuery('(max-width: 1279px)')

  const parts = useMemo(() => (problem ? splitStatement(problem.statement, problem.title) : null), [problem])

  useEffect(() => {
    if (problem) document.title = `${problem.number ? `${problem.number}. ` : ''}${problem.title} · CodeDrill`
    return () => {
      document.title = 'CodeDrill'
    }
  }, [problem])

  /* ─── 결과 창 ─── */

  const showDrawer = useCallback(
    (tab: DrawerTab) => {
      setDrawerTab(tab)
      setMobileTab('result')
      if (drawerPanel.current?.isCollapsed()) drawerPanel.current.expand()
    },
    [drawerPanel],
  )

  // 판정이 끝나면 결과 창을 펼치고 판정을 보여 준다 (UI 디자인 문서 §8.3). 기록도 새로 읽는다.
  // 문제를 열었다 — 이 화면이 설 때 한 번, 어디서 왔는지와 함께 (§16.1 problem_open)
  useEffect(() => {
    if (problem) trackOnce(`open:${problem.id}:${mountId}`, 'problem_open', { source: takeOpenSource(), problemId: problem.id })
  }, [problem?.id])

  // 결과 창의 리플레이 탭을 실제로 열었을 때 (§16.1 replay_opened — 진입 경로 drawer). 탭이 숨은 채로 그려진
  // 것은 연 것이 아니다
  const replayShown = drawerTab === 'replay' && (!isMobile || mobileTab === 'result')
  useEffect(() => {
    if (replayShown && submissionId && trace) trackOnce(`replay-drawer:${submissionId}`, 'replay_opened', { entry: 'drawer', traceType: trace.status })
  }, [replayShown, submissionId, trace?.traceId])

  const completed = submission?.status === 'COMPLETED' || submission?.status === 'SYSTEM_ERROR'
  // 판정을 봤다 — 이 화면에서 낸 제출만, 낸 때부터 판정이 보일 때까지 (§16.1 verdict_viewed, 결과 전달)
  useEffect(() => {
    if (!completed || !submission) return
    const sentAt = submittedAt.current.get(submission.id)
    if (sentAt === undefined) return
    submittedAt.current.delete(submission.id)
    track('verdict_viewed', { verdict: submission.verdict ?? submission.status, latency: Date.now() - sentAt })
  }, [completed, submission?.id])

  useEffect(() => {
    if (!completed) return
    void queryClient.invalidateQueries({ queryKey: ['submissions', slug] })
    if (submission?.mine !== false && drawerTab !== 'replay') showDrawer('verdict')
    // 판정이 바뀌는 순간에만 — 사용자가 다른 탭으로 옮긴 뒤에는 다시 끌고 오지 않는다.
  }, [completed, submission?.id])

  /* ─── 행동 ─── */

  /** 로그인하고 이 화면으로 돌아온다. 쓰던 코드는 이 기기에 있다. */
  const toLogin = () => navigate(`/login?next=${encodeURIComponent(location + window.location.search)}`)

  const run = () => {
    if (!signedIn) return toLogin()
    showDrawer('tests')
    testPanel.current?.run()
  }

  const submit = async () => {
    if (!signedIn) return toLogin()
    if (!problem || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const created = await createSubmission(problem.id, problem.version, language, workspace.source)
      submittedAt.current.set(created.id, Date.now())
      track('submission_created', { problem: problem.id, language })
      openSubmission(created.id)
      void queryClient.invalidateQueries({ queryKey: ['submissions', slug] })
    } catch (e) {
      setError(e instanceof Error ? e.message : '제출에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const openSubmission = (id: string, step: number | null = null) => {
    setSubmissionId(id)
    setInitialStep(step)
    setReplayStep(null)
    setParam('submission', id)
    setParam('step', step === null ? null : String(step))
    showDrawer(step === null ? 'verdict' : 'replay')
  }

  const changeLanguage = (next: SubmissionLanguage) => {
    languageChosen.current = true
    setLanguage(next)
    setParam('lang', next === 'KOTLIN' ? null : next)
  }

  const toggleProblemPane = () => {
    const panel = problemPanel.current
    if (!panel) return
    if (panel.isCollapsed()) panel.expand()
    else panel.collapse()
  }

  useShortcuts({
    run,
    submit: () => void submit(),
    toggleDrawer: () => {
      if (isMobile) return setMobileTab((tab) => (tab === 'result' ? 'code' : 'result'))
      const panel = drawerPanel.current
      if (!panel) return
      if (panel.isCollapsed()) panel.expand()
      else panel.collapse()
    },
    toggleProblemPane,
    focusProblem: () => {
      if (isMobile) setMobileTab('problem')
      else if (problemPanel.current?.isCollapsed()) problemPanel.current.expand()
      requestAnimationFrame(() => problemPane.current?.focus())
    },
    focusEditor: () => {
      if (isMobile) setMobileTab('code')
      requestAnimationFrame(() => editor.current?.focus())
    },
  })

  /* ─── 레이아웃 기억 (UI 디자인 문서 §8.3 — 사용자+기기) ─── */

  // 사용자가 직접 움직인 크기만 기억한다. 태블릿 폭에서 자동으로 접은 것까지 저장하면 데스크톱에서
  // 다시 열 때도 문제 창이 접혀 있다.
  const horizontal = useDefaultLayout({ id: 'solve-horizontal', storage: safeStorage, onlySaveAfterUserInteractions: true })
  const vertical = useDefaultLayout({ id: 'solve-vertical', storage: safeStorage, onlySaveAfterUserInteractions: true })

  // 768~1023 은 문제 창을 접고 시작한다 (§8.1). 사용자가 펼치면 그대로 둔다.
  const collapsedOnce = useRef(false)
  useEffect(() => {
    if (isTablet && !isMobile && !collapsedOnce.current && problemPanel.current) {
      collapsedOnce.current = true
      problemPanel.current.collapse()
    }
  }, [isTablet, isMobile, problemPanel])

  /* ─── 그리기 ─── */

  if (problemQuery.isError) {
    return (
      <div className={styles.page}>
        <div className={styles.fallback}>
          <EmptyState
            title="문제를 찾지 못했습니다"
            action={
              <Link href="/" className="linklike">
                문제 목록으로
              </Link>
            }
          >
            주소가 바뀌었거나 공개되지 않은 문제입니다.
          </EmptyState>
        </div>
      </div>
    )
  }

  const inFlight = submission ? IN_FLIGHT.has(submission.status) : false
  const viewed = history.find((item) => item.id === viewingCode) ?? null

  const toolbar = (
    <header className={styles.toolbar}>
      <div className={styles.toolbarStart}>
        <Link href="/problems" className={styles.back} aria-label="문제 목록으로">
          <ArrowLeft size={18} aria-hidden="true" />
        </Link>
        {!isMobile && (
          <IconButton
            label={`${problemCollapsed ? '문제 창 펼치기' : '문제 창 접기'} (${MOD}\\)`}
            icon={problemCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
            onClick={toggleProblemPane}
          />
        )}
        <h1 className={styles.title}>
          {problem ? (
            <>
              {problem.number && <span className={styles.number}>{problem.number}</span>}
              {problem.title}
            </>
          ) : (
            <Skeleton width={160} height={18} />
          )}
        </h1>
      </div>

      <div className={styles.toolbarEnd}>
        {!isMobile && (signedIn ? <SaveStatus state={workspace.saveState} /> : <LocalSaveStatus saved={workspace.localSaved} />)}
        <Select
          label="언어"
          hideLabel
          className={styles.language}
          value={language}
          onChange={(event) => changeLanguage(event.target.value as SubmissionLanguage)}
        >
          {LANGUAGES.map((value) => (
            <option key={value} value={value}>
              {LANGUAGE_LABEL[value]}
            </option>
          ))}
        </Select>
        {/* 모바일은 읽기·제출 확인이 중심이다 (§8.1). 설정·대결·코칭은 데스크톱에서 */}
        {!isMobile && !signedIn && (
          <IconButton label="에디터 설정" icon={<Settings size={18} />} onClick={() => setSettingsOpen(true)} />
        )}
        {!isMobile && signedIn && (
          <>
            <IconButton label="에디터 설정" icon={<Settings size={18} />} onClick={() => setSettingsOpen(true)} />
            <IconButton
              label="이 문제로 미니 대결 열기"
              icon={<Swords size={18} />}
              onClick={() => setDuelOpen(true)}
              disabled={!problem}
            />
            <Button
              variant={coachingOpen ? 'secondary' : 'tertiary'}
              icon={<GraduationCap size={16} />}
              aria-pressed={coachingOpen}
              onClick={() => setCoachingOpen((open) => !open)}
            >
              {isNarrowDesktop ? <span className="visually-hidden">코칭</span> : '코칭'}
            </Button>
          </>
        )}
        {!signedIn ? (
          <Button variant="primary" icon={<LogIn size={16} />} onClick={toLogin} title="로그인하면 실행·제출할 수 있습니다. 쓰던 코드는 이어집니다">
            {isMobile ? '로그인' : '로그인하고 실행·제출'}
          </Button>
        ) : isMobile ? (
          <IconButton label="실행" icon={<Play size={18} />} onClick={run} disabled={!problem || !workspace.source.trim() || running} />
        ) : (
          <Button
            icon={<Play size={16} />}
            onClick={run}
            loading={running}
            disabled={!problem || !workspace.source.trim()}
            title={`실행 (${MOD}↵)`}
          >
            실행
          </Button>
        )}
        {signedIn && (
          <Button
            variant="primary"
            icon={<Send size={16} />}
            onClick={() => void submit()}
            loading={submitting}
            disabled={!problem || inFlight}
            title={`제출 (${MOD}⇧↵)`}
          >
            제출
          </Button>
        )}
      </div>
    </header>
  )

  const problemPaneContent = (
    <div
      ref={problemPane}
      className={styles.problemPane}
      tabIndex={-1}
      aria-label={`문제 창 (${ALT}1)`}
      role="region"
    >
      {!problem || !parts ? (
        <div className={styles.paneBody} role="status" aria-busy="true">
          <span className="visually-hidden">문제를 불러오는 중</span>
          <Skeleton width="60%" height={20} />
          <Skeleton />
          <Skeleton width="90%" />
          <Skeleton width="75%" />
        </div>
      ) : (
        <Tabs
          label="문제 창"
          value={problemTab}
          onChange={setProblemTab}
          items={
            signedIn
              ? [
                  { key: 'statement', label: '문제' },
                  { key: 'editorial', label: '해설' },
                  { key: 'discussion', label: '질문' },
                  { key: 'submissions', label: `제출${history.length ? ` ${history.length}` : ''}` },
                ]
              : // 해설·질문·제출 기록은 계정의 것이다 — 둘러보는 동안은 지문만
                [{ key: 'statement', label: '문제' }]
          }
        >
          <KeepAlive active={problemTab} keys={['statement', 'editorial', 'discussion', 'submissions']}>
            {(tab) => (
              <div className={styles.paneBody}>
                {tab === 'statement' && (
                  <>
                    {/* 풀기 전에 묻는 것은 지문 위다. 접힌 채로 시작한다 (FR-803 — 관문이 아니다). */}
                    {signedIn && <PreQuestionPanel problemId={problem.id} />}
                    <StatementView problem={problem} body={parts.body} />
                  </>
                )}
                {tab === 'editorial' && (
                  <EditorialPanel
                    problemId={problem.id}
                    problemSignature={problem.signature}
                    sampleArgs={problem.samples[0]?.args ?? null}
                  />
                )}
                {tab === 'discussion' && (
                  <DiscussionPanel
                    problemId={problem.id}
                    submission={submission}
                    replayStep={trace ? replayStep : null}
                    onOpenReplay={openSubmission}
                  />
                )}
                {tab === 'submissions' && (
                  <>
                    <HistoryPanel
                      submissions={history}
                      currentId={submissionId}
                      onOpen={(id) => openSubmission(id)}
                      onView={setViewingCode}
                    />
                    <Link href={`/submissions?problem=${problem.id}`} className={styles.allSubmissions}>
                      이 문제의 제출 전체 보기
                    </Link>
                    {viewed && (
                      <CodeView
                        submission={viewed}
                        previous={previousOf(history, viewed)}
                        onClose={() => setViewingCode(null)}
                      />
                    )}
                  </>
                )}
              </div>
            )}
          </KeepAlive>
        </Tabs>
      )}
    </div>
  )

  const editorContent = (
    <div className={styles.editorPane}>
      <SaveIndicatorBanner state={workspace.saveState} onResolve={workspace.resolveConflict} />
      {workspace.localDraft !== null && (
        <div className={styles.saveBanner}>
          <InlineAlert
            tone="warning"
            title="로그인 전에 이 기기에서 쓴 코드가 있습니다"
            action={
              <div className={styles.mergeActions}>
                <Button size="dense" variant="primary" onClick={workspace.takeLocalDraft}>
                  그 코드로 이어 쓰기
                </Button>
                <Button size="dense" onClick={workspace.keepServerDraft}>
                  저장된 초안 유지
                </Button>
              </div>
            }
          >
            저장된 초안과 다릅니다. 고르지 않은 쪽은 사라집니다.
          </InlineAlert>
        </div>
      )}
      <div className={styles.editor}>
        <Suspense fallback={<p className={styles.editorLoading}>에디터를 불러오는 중…</p>}>
          <MonacoWorkspace
            source={workspace.source}
            language={EDITOR_LANGUAGE[language]}
            onChange={workspace.edit}
            settings={settings}
            onReady={(handle) => {
              editor.current = handle
            }}
            label={`코드 편집기 (${ALT}2)`}
          />
        </Suspense>
      </div>
    </div>
  )

  const drawerContent = !signedIn ? (
    <section className={styles.drawer} aria-label="결과 창">
      <div className={styles.anonymous}>
        <EmptyState
          title="로그인하면 예제로 실행하고 제출할 수 있습니다"
          action={
            <Button variant="primary" icon={<LogIn size={16} />} onClick={toLogin}>
              로그인 · 가입
            </Button>
          }
        >
          쓰던 코드는 이 기기에 남아, 로그인하고 돌아오면 그대로 이어집니다.
        </EmptyState>
      </div>
    </section>
  ) : (
    <section className={styles.drawer} aria-label="결과 창">
      {problem && (
        <Tabs
          label={`결과 창 (${MOD}J)`}
          value={drawerTab}
          onChange={(tab) => showDrawer(tab)}
          items={[
            { key: 'tests', label: running ? '테스트 · 실행 중' : '테스트' },
            { key: 'verdict', label: <VerdictTabLabel submission={submission} /> },
            { key: 'replay', label: '리플레이' },
          ]}
        >
          <div className={styles.drawerBody}>
            {/* 테스트 패널은 늘 붙어 있어야 한다 — ⌘↵ 가 어느 탭에서든 이 패널의 실행을 부른다. */}
            <div hidden={drawerTab !== 'tests'}>
              <TestPanel
                ref={testPanel}
                problem={problem}
                language={language}
                source={workspace.source}
                onRunningChange={setRunning}
              />
            </div>
            <div hidden={drawerTab !== 'verdict'} aria-live="polite">
              {submission ? (
                <VerdictPanel
                  submission={submission}
                  problem={problem}
                  hasTrace={trace !== null && trace.status !== 'EMPTY'}
                  onJumpToLine={(line, column) => {
                    if (isMobile) setMobileTab('code')
                    requestAnimationFrame(() => editor.current?.revealLine(line, column ?? 1))
                  }}
                  onOpenReplay={() => showDrawer('replay')}
                  onOpenEditorial={() => {
                    setProblemTab('editorial')
                    if (isMobile) setMobileTab('problem')
                    else if (problemPanel.current?.isCollapsed()) problemPanel.current.expand()
                  }}
                  onResubmit={() => void submit()}
                />
              ) : (
                <EmptyState title="아직 제출하지 않았습니다">
                  제출하면 채점 단계와 그룹별 결과가 여기 나옵니다. ({MOD}⇧↵)
                </EmptyState>
              )}
            </div>
            <div hidden={drawerTab !== 'replay'}>
              {trace && submissionId ? (
                <ReplayView
                  submissionId={submissionId}
                  manifest={trace}
                  input={sampleArray(problem.samples[0]?.args[0])}
                  initialStep={initialStep}
                  onStepChange={setReplayStep}
                  mine={submission?.mine !== false}
                  expandHref={`/submissions/${submissionId}/replay?step=${replayStep ?? 0}`}
                />
              ) : (
                <EmptyState title="리플레이가 아직 없습니다">
                  {submission && inFlight
                    ? '채점이 끝나면 실행 과정을 여기서 다시 볼 수 있습니다.'
                    : '제출하면 실행 과정을 걸음별로 다시 볼 수 있습니다.'}
                </EmptyState>
              )}
              {parts?.instrumentation && (
                <details className={styles.instrumentation}>
                  <summary>리플레이 계측 (선택) — 내 코드에서 직접 기록하기</summary>
                  <Markdown source={parts.instrumentation} />
                </details>
              )}
            </div>
          </div>
        </Tabs>
      )}
    </section>
  )

  const coaching = coachingOpen && problem && (
    <aside className={styles.coaching} aria-label="코칭">
      {/* 제목은 안의 코칭 패널이 단다. 여기는 닫기만 */}
      <div className={styles.coachingHead}>
        <Button variant="tertiary" size="dense" onClick={() => setCoachingOpen(false)}>
          코칭 닫기
        </Button>
      </div>
      {/* 열어야 개입한다 (FR-802 — 관문이 아니다) */}
      <CoachingPanel
        problemId={problem.id}
        resume={resumeCoaching}
        onOpenProblem={(id) => navigate(`/problems/${id}/solve`)}
      />
    </aside>
  )

  return (
    <div className={styles.page}>
      {toolbar}
      {error && (
        <div className={styles.alert}>
          <InlineAlert tone="danger" title={error} action={<Button size="dense" onClick={() => setError(null)}>닫기</Button>} />
        </div>
      )}

      {isMobile ? (
        // 모바일은 문제·코드·결과를 탭으로 바꾼다. 장시간 코딩은 데스크톱이 1차 환경이다 (§8.1).
        <div className={styles.mobile}>
          <Tabs
            label="풀이 화면"
            value={mobileTab}
            onChange={setMobileTab}
            items={[
              { key: 'problem', label: '문제' },
              { key: 'code', label: '코드' },
              { key: 'result', label: '결과' },
            ]}
          >
            <div className={styles.mobileBody}>
              <div hidden={mobileTab !== 'problem'} className={styles.mobileFill}>
                {problemPaneContent}
              </div>
              <div hidden={mobileTab !== 'code'} className={styles.mobileFill}>
                <p className={styles.mobileHint}>긴 코드는 데스크톱에서 쓰기 편합니다.</p>
                {editorContent}
              </div>
              <div hidden={mobileTab !== 'result'} className={styles.mobileFill}>
                {drawerContent}
              </div>
            </div>
          </Tabs>
          {coaching}
        </div>
      ) : (
        <div className={styles.body}>
          <Group
            orientation="horizontal"
            id="solve-horizontal"
            className={styles.group}
            defaultLayout={horizontal.defaultLayout}
            onLayoutChanged={horizontal.onLayoutChanged}
          >
            <Panel
              id="problem"
              panelRef={problemPanel}
              collapsible
              collapsedSize={0}
              defaultSize={isNarrowDesktop ? '34%' : '38%'}
              minSize="280px"
              maxSize="55%"
              onResize={(size) => setProblemCollapsed(size.asPercentage === 0)}
            >
              {problemPaneContent}
            </Panel>
            <Separator className={styles.separatorVertical} aria-label="문제와 에디터 사이 크기 조절" />
            <Panel id="work" minSize="360px">
              <Group
                orientation="vertical"
                id="solve-vertical"
                className={styles.group}
                defaultLayout={vertical.defaultLayout}
                onLayoutChanged={vertical.onLayoutChanged}
              >
                <Panel id="editor" minSize="120px">
                  {editorContent}
                </Panel>
                <Separator className={styles.separatorHorizontal} aria-label="에디터와 결과 창 사이 크기 조절" />
                <Panel
                  id="drawer"
                  panelRef={drawerPanel}
                  collapsible
                  collapsedSize="41px"
                  defaultSize="32%"
                  minSize="140px"
                  maxSize="75%"
                  onResize={(size) => setDrawerCollapsed(size.inPixels <= 42)}
                  className={drawerCollapsed ? styles.drawerCollapsed : undefined}
                >
                  {drawerContent}
                </Panel>
              </Group>
            </Panel>
          </Group>
          {coaching}
        </div>
      )}

      {problem && <DuelDialog problemId={problem.id} open={duelOpen} onClose={() => setDuelOpen(false)} />}
      <EditorSettingsDialog
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onReset={workspace.reset}
      />
    </div>
  )
}

/* ─── 작은 조각 ─── */

/** 로그인 전 저장 상태. 서버가 아니라 이 기기다 — 그렇게 말한다. 못 적었으면 탭을 닫으면 사라진다고. */
function LocalSaveStatus({ saved }: { saved: boolean | null }) {
  if (saved === null) return <span className={styles.saveState}>로그인 전 — 이 기기에 저장</span>
  return (
    <span className={saved ? styles.saveState : styles.saveFailed} role="status">
      {saved ? '이 기기에 저장됨' : '이 브라우저는 저장할 수 없어 탭을 닫으면 사라집니다'}
    </span>
  )
}

/** 툴바의 저장 상태 — 버튼이 아니라 조용한 글 (디자인 설계서 §6.2 Status). */
function SaveStatus({ state }: { state: ReturnType<typeof useWorkspaceSource>['saveState'] }) {
  const label =
    state.status === 'saving'
      ? '저장 중…'
      : state.status === 'saved'
        ? '저장됨'
        : state.status === 'failed'
          ? '저장 실패'
          : ''
  return (
    <span className={state.status === 'failed' ? styles.saveFailed : styles.saveState} role="status">
      {label}
    </span>
  )
}

/**
 * 충돌과 실패만 에디터 위 배너로 남긴다. 사라지는 알림이면 고르기 전에 놓친다 — 그 순간
 * 잃는 것이 작성 중이던 코드다 (Workspace.tsx SaveIndicator 와 같은 규칙).
 */
function SaveIndicatorBanner({
  state,
  onResolve,
}: {
  state: ReturnType<typeof useWorkspaceSource>['saveState']
  onResolve: (code: string | null, version: number) => void
}) {
  if (state.status !== 'conflict' && state.status !== 'failed') return null
  return (
    <div className={styles.saveBanner}>
      <SaveIndicator state={state} onResolve={(current, version) => onResolve(current?.code ?? null, version)} />
    </div>
  )
}

function VerdictTabLabel({ submission }: { submission: Submission | null }) {
  if (!submission) return <>판정</>
  if (IN_FLIGHT.has(submission.status)) return <JudgeStatusBadge status={submission.status} />
  if (submission.verdict) return <VerdictBadge verdict={submission.verdict} />
  return <>판정</>
}

/**
 * 한 번 연 탭은 붙여 둔다. 탭을 오가며 질문 초안이나 스크롤 자리를 잃지 않게 하고, 열지 않은
 * 탭은 요청도 하지 않는다.
 */
function KeepAlive<K extends string>({
  active,
  keys,
  children,
}: {
  active: K
  keys: K[]
  children: (key: K) => ReactNode
}) {
  const [visited, setVisited] = useState<Set<K>>(() => new Set([active]))
  useEffect(() => {
    setVisited((prev) => (prev.has(active) ? prev : new Set(prev).add(active)))
  }, [active])
  return (
    <>
      {keys
        .filter((key) => visited.has(key) || key === active)
        .map((key) => (
          <div key={key} hidden={key !== active} className={styles.keepAlive}>
            {children(key)}
          </div>
        ))}
    </>
  )
}

/** 기록에서 바로 다음(= 시간상 이전) 제출. 목록은 최신순이다. */
function previousOf(history: Submission[], current: Submission): Submission | null {
  const index = history.findIndex((item) => item.id === current.id)
  return index >= 0 ? (history[index + 1] ?? null) : null
}

/** 저장소를 못 쓰는 브라우저에서도 레이아웃 기억만 빠지고 화면은 그대로 돈다. */
const safeStorage = {
  getItem: (key: string) => {
    try {
      return localStorage.getItem(key)
    } catch {
      return null
    }
  },
  setItem: (key: string, value: string) => {
    try {
      localStorage.setItem(key, value)
    } catch {
      // 기억하지 못할 뿐이다
    }
  },
}
