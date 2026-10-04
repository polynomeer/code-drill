import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, PenLine } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Group, Panel, Separator, useDefaultLayout } from 'react-resizable-panels'
import { Link } from 'wouter'
import { getProblem, getSubmission, getSubmissionSource, getTraceManifest } from '../../api/client'
import { Badge, EmptyState, Skeleton, Tabs, VerdictBadge } from '../../design'
import { useMediaQuery } from '../../shared/useMediaQuery'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { CodeTracePane } from './CodeTracePane'
import { DivergenceCard, useDivergence } from './DivergenceCard'
import { PredictNext } from './PredictNext'
import { ReplayTimeline } from './ReplayTimeline'
import { StateInspector } from './StateInspector'
import { applyAll } from './reducer'
import { EventFallback, renderersFor } from './renderers'
import {
  EVENT_LABEL,
  describeStep,
  linesTouching,
  linesWithEvents,
  markersOf,
  nextMarker,
  nextOnLine,
  sampleArray,
} from './timeline'
import { schemaSupported } from './traceTypes'
import type { TraceEventType, TraceManifest } from './traceTypes'
import { useReplayPlayer } from './useReplayPlayer'
import { kindsIn, useTrace } from './useTrace'
import type { Problem, Submission } from '../../shared/types'
import styles from './ReplayPage.module.css'
import { track, trackOnce } from '../../shared/analytics'

/**
 * R-01 실행 리플레이 `/submissions/:id/replay?step=` (UI 디자인 문서 §5, 디자인 설계서 §8).
 *
 * > 사용자가 "현재 보이는 상태가 어떤 코드와 입력 때문에 만들어졌는가"를 잃지 않고 추적하게 합니다.
 *
 * 네 영역 — Code 30 / Canvas 48 / Inspector 22 + 아래 Timeline — 이 **한 걸음**을 함께 가리킨다
 * (§11.2 "같은 seq 에서 네 Pane 상태 일치"). 걸음은 [useReplayPlayer] 하나가 들고, 각 pane 은 그
 * 걸음에서 자기 것을 계산할 뿐이다.
 *
 * 걸음 N 의 상태를 아직 받지 않은 구간에서 그리면 앞 구간의 상태가 "N 의 상태"인 척하게 된다.
 * 그래서 받는 동안에는 캔버스를 그리지 않고 기다린다고 말한다.
 */
export function ReplayPage({ id }: { id: string }) {
  const submissionQuery = useQuery({ queryKey: ['submission', id], queryFn: () => getSubmission(id) })
  const submission = submissionQuery.data
  const traceQuery = useQuery({ queryKey: ['submission', id, 'trace'], queryFn: () => getTraceManifest(id) })
  const problemQuery = useQuery({
    queryKey: ['problem', submission?.problemId],
    queryFn: () => getProblem(submission!.problemId),
    enabled: !!submission,
  })
  const mine = submission?.mine !== false
  const sourceQuery = useQuery({
    queryKey: ['submission', id, 'source'],
    queryFn: () => getSubmissionSource(id),
    enabled: !!submission && mine,
  })

  if (submissionQuery.isError || traceQuery.isError) {
    return (
      <Frame id={id}>
        <EmptyState
          title="리플레이를 열지 못했습니다"
          action={
            <Link href="/submissions" className="linklike">
              내 제출로
            </Link>
          }
        >
          지웠거나 볼 수 없는 제출입니다.
        </EmptyState>
      </Frame>
    )
  }

  if (!submission || traceQuery.isPending || problemQuery.isPending || (mine && sourceQuery.isPending)) {
    return (
      <Frame id={id}>
        <div className={styles.loading} role="status" aria-busy="true">
          <span className="visually-hidden">불러오는 중</span>
          <Skeleton height={320} />
          <Skeleton height={320} />
          <Skeleton height={320} />
        </div>
      </Frame>
    )
  }

  const manifest = traceQuery.data
  if (!manifest || manifest.status === 'EMPTY') {
    return (
      <Frame id={id} submission={submission}>
        <EmptyState
          title="이 제출에는 리플레이가 없습니다"
          action={
            <Link href={`/submissions/${id}`} className="linklike">
              제출로 돌아가기
            </Link>
          }
        >
          {manifest?.diagnostics ??
            '채점이 끝나지 않았거나, 코드가 리플레이 계측을 부르지 않았습니다. 풀이 화면의 리플레이 탭에 계측 방법이 있습니다.'}
        </EmptyState>
      </Frame>
    )
  }

  if (manifest.status === 'INVALID' || !schemaSupported(manifest.schemaVersion)) {
    return (
      <Frame id={id} submission={submission}>
        <div className={styles.fallback}>
          <EventFallback
            events={manifest.summary}
            reason={
              manifest.status === 'INVALID'
                ? `트레이스를 신뢰할 수 없어 상태를 그리지 않습니다: ${manifest.diagnostics ?? ''}`
                : `이 버전(${manifest.schemaVersion})을 아직 그릴 수 없습니다. 걸음 목록만 보입니다.`
            }
          />
        </div>
      </Frame>
    )
  }

  return (
    <Replay
      submission={submission}
      manifest={manifest}
      problem={problemQuery.data ?? null}
      source={mine ? (sourceQuery.data ?? null) : null}
    />
  )
}

function Replay({
  submission,
  manifest,
  problem,
  source,
}: {
  submission: Submission
  manifest: TraceManifest
  problem: Problem | null
  source: string | null
}) {
  const id = submission.id
  const mine = submission.mine !== false
  const wide = useMediaQuery('(min-width: 1024px)')
  const total = manifest.eventCount
  const divergence = useDivergence(id)
  const divergedAtSeq = mine && divergence?.outcome === 'DIVERGED' ? divergence.divergedAtSeq : null

  const [initialStep] = useState(() => Number(new URLSearchParams(window.location.search).get('step') ?? 0))
  const [selected, setSelected] = useState<string | null>(null)
  const [focusLine, setFocusLine] = useState<{ line: number } | null>(null)
  const [mobileTab, setMobileTab] = useState<'canvas' | 'code'>('canvas')

  // 마커는 모드에 따라 바뀌지만 걸음은 그대로다. player 가 모드를 들고 있어 두 번 계산한다.
  const summaryMarkers = useMemo(() => markersOf(manifest.summary, divergedAtSeq, 'summary'), [manifest.summary, divergedAtSeq])
  const allMarkers = useMemo(() => markersOf(manifest.summary, divergedAtSeq, 'all'), [manifest.summary, divergedAtSeq])
  const player = useReplayPlayer({ total, markers: summaryMarkers, initialStep })
  const markers = player.mode === 'summary' ? summaryMarkers : allMarkers
  const { step, seek } = player

  const { events, error, ensureLoaded } = useTrace(id, manifest)
  useEffect(() => {
    void ensureLoaded(step)
  }, [step, ensureLoaded])

  const loaded = step <= events.length
  const state = useMemo(() => applyAll(events, step), [events, step])
  const renderers = useMemo(() => renderersFor(kindsIn(events, manifest)), [events, manifest])
  const current = step === 0 ? null : (events[step - 1] ?? manifest.summary.find((event) => event.seq === step) ?? null)
  const input = useMemo(() => sampleArray(problem?.samples[0]?.args[0]), [problem])
  const eventLines = useMemo(() => linesWithEvents(events, manifest.summary), [events, manifest.summary])
  const touchedLines = useMemo(
    () => (selected ? linesTouching(events, step, selected) : new Set<number>()),
    [events, step, selected],
  )
  const choiceTypes = useMemo(
    () => [...new Set(manifest.summary.concat(events).map((event) => event.eventType))].sort(),
    [events, manifest.summary],
  )

  const showLine = useCallback(
    (line: number) => {
      player.pause()
      if (!wide) setMobileTab('code')
      setFocusLine({ line })
    },
    [player, wide],
  )

  const onLine = (line: number) => {
    const next = nextOnLine(events, manifest.summary, line, step)
    if (next === null) return
    const target = events[next - 1] ?? manifest.summary.find((event) => event.seq === next)
    // 코드 → 상태 (§16.1 code_trace_link_used — 양방향 연결 가치)
    track('code_trace_link_used', { direction: 'code-to-state', eventType: target?.eventType ?? 'unknown' })
    seek(next, 'line')
  }

  const onSelect = (key: string) => {
    player.pause()
    const selecting = selected !== key
    setSelected(selecting ? key : null)
    // 상태 → 코드: 고른 칸을 건드린 줄이 칠해진다
    if (selecting) track('code_trace_link_used', { direction: 'state-to-code', eventType: key.split(':')[0] ?? 'unknown' })
  }

  // 리플레이를 열었다 — 주소에 걸음이 있으면 누가 건넨 링크로 온 것이다 (§16.1 replay_opened)
  useEffect(() => {
    trackOnce(`replay:${id}`, 'replay_opened', { entry: initialStep > 0 ? 'link' : 'page', traceType: manifest.status })
  }, [id])

  useReplayKeys({
    onStep: (delta) => seek(step + delta, 'key'),
    onImportant: (direction) => seek(nextMarker(summaryMarkers, step, direction), 'key'),
    onHome: () => seek(0, 'key'),
    onEnd: () => seek(total, 'key'),
    onToggle: player.toggle,
  })

  const warnings = [
    error,
    state.unknown > 0 ? `해석하지 못한 이벤트 ${state.unknown}건은 상태에 반영하지 않았습니다.` : null,
    manifest.truncated ? '이벤트 예산을 넘겨 이후가 잘렸습니다. 요약만 완전합니다.' : null,
  ].filter((warning): warning is string => !!warning)

  const caseLabel = `${manifest.caseId}${input.length > 0 ? ` · 입력 [${input.join(', ')}]` : ''}`
  const sentence = current ? describeStep(current) : '재생 전 — 처음 상태'

  const code = (
    <section className={styles.pane} aria-labelledby="replay-code">
      <h2 id="replay-code" className={styles.paneHeading}>
        코드
      </h2>
      <CodeTracePane
        source={source}
        activeLine={current?.sourceLine ?? null}
        touchedLines={touchedLines}
        eventLines={eventLines}
        focusLine={focusLine}
        onLine={onLine}
        onUserScroll={player.pause}
      />
    </section>
  )

  const canvas = (
    <section className={styles.pane} aria-labelledby="replay-canvas" data-step={step}>
      <h2 id="replay-canvas" className={styles.paneHeading}>
        상태 <span className={styles.caseId}>{manifest.caseId}</span>
      </h2>
      <div className={styles.canvas}>
        {!loaded ? (
          <div role="status" className={styles.canvasLoading}>
            단계 {step}의 상태를 불러오는 중입니다…
          </div>
        ) : renderers.length === 0 ? (
          <EventFallback events={manifest.summary} reason="이 트레이스의 자료구조는 아직 그릴 수 없습니다." />
        ) : (
          renderers.map((renderer) => (
            <div key={renderer.kind} className={styles.renderer}>
              <h3 className={styles.rendererTitle}>{renderer.title}</h3>
              {renderer.render(state, input, { selected, onSelect })}
            </div>
          ))
        )}
      </div>
    </section>
  )

  const inspector = (
    <StateInspector
      current={current}
      events={events}
      step={step}
      divergedAtSeq={divergedAtSeq}
      selected={selected}
      onClearSelection={() => setSelected(null)}
      onSeek={(target) => seek(target, 'list')}
      onShowLine={showLine}
      warnings={warnings}
    >
      {mine && (
        <>
          <DivergenceCard submissionId={id} onSeek={(target) => seek(target, 'divergence')} onShowLine={showLine} caseLabel={caseLabel} />
          {/* 재생 위치 곁에 둔다. 다음을 누르기 전에 눈에 들어와야 예측이지, 지나간 뒤에 물으면 회상이다 */}
          <PredictNext
            submissionId={id}
            next={events[step] ?? null}
            choices={choiceTypes}
            labelOf={(type) => EVENT_LABEL[type as TraceEventType] ?? type}
          />
        </>
      )}
    </StateInspector>
  )

  return (
    <Frame id={id} submission={submission} step={step}>
      {/* 재생 중에는 읽지 않는다 — 걸음마다 읽으면 화면 낭독기가 재생을 따라잡지 못한다 (§8.5) */}
      <p className="visually-hidden" aria-live="polite">
        {player.playing ? '' : sentence}
      </p>

      {wide ? (
        <ReplayPanels code={code} canvas={canvas} inspector={inspector} />
      ) : (
        <div className={styles.mobile}>
          <Tabs
            label="리플레이 보기"
            value={mobileTab}
            onChange={setMobileTab}
            items={[
              { key: 'canvas', label: '상태' },
              { key: 'code', label: '코드' },
            ]}
          >
            <div className={styles.mobileBody}>{mobileTab === 'canvas' ? canvas : code}</div>
          </Tabs>
          {/* Inspector bottom sheet (ui-overhaul.md §6.4) — 접힌 채로 지금 걸음의 문장만 보인다 */}
          <details className={styles.sheet}>
            <summary className={styles.sheetSummary}>
              <span className={styles.sheetHandle} aria-hidden="true" />
              {sentence}
            </summary>
            <div className={styles.sheetBody}>{inspector}</div>
          </details>
        </div>
      )}

      <ReplayTimeline
        step={step}
        total={total}
        markers={markers}
        important={summaryMarkers}
        playing={player.playing}
        speed={player.speed}
        mode={player.mode}
        valueText={`${step} / ${total} — ${sentence}`}
        loading={!loaded}
        onSeek={seek}
        onToggle={player.toggle}
        onSpeed={player.setSpeed}
        onMode={player.setMode}
      />
    </Frame>
  )
}

function ReplayPanels({ code, canvas, inspector }: { code: React.ReactNode; canvas: React.ReactNode; inspector: React.ReactNode }) {
  const layout = useDefaultLayout({ id: 'replay-horizontal', storage: safeStorage, onlySaveAfterUserInteractions: true })
  return (
    <div className={styles.body}>
      <Group
        orientation="horizontal"
        id="replay-horizontal"
        className={styles.group}
        defaultLayout={layout.defaultLayout}
        onLayoutChanged={layout.onLayoutChanged}
      >
        <Panel id="code" defaultSize="31%" minSize="220px">
          {code}
        </Panel>
        <Separator className={styles.separator} aria-label="코드와 상태 사이 크기 조절" />
        <Panel id="canvas" defaultSize="48%" minSize="280px">
          {canvas}
        </Panel>
        <Separator className={styles.separator} aria-label="상태와 검사기 사이 크기 조절" />
        <Panel id="inspector" defaultSize="21%" minSize="220px">
          <section className={styles.pane} aria-label="검사기">
            {inspector}
          </section>
        </Panel>
      </Group>
    </div>
  )
}

/** 헤더 (디자인 설계서 §8.2 "문제·제출·판정·공유·닫기"). 전역 헤더는 이 화면에서 접힌다. */
function Frame({
  id,
  submission,
  step,
  children,
}: {
  id: string
  submission?: Submission
  step?: number
  children: React.ReactNode
}) {
  const index = useProblemIndex()
  return (
    <div className={styles.page}>
      <header className={styles.toolbar}>
        <div className={styles.toolbarStart}>
          <Link href={`/submissions/${id}`} className={styles.back} aria-label="제출로 돌아가기">
            <ArrowLeft size={18} aria-hidden="true" />
          </Link>
          <h1 className={styles.title}>
            {submission ? problemLabel(index, submission.problemId) : '실행 리플레이'}
            <span className={styles.titleSuffix}> · 리플레이</span>
          </h1>
          {submission?.verdict && <VerdictBadge verdict={submission.verdict} />}
          {submission && submission.mine === false && <Badge>공유된 제출</Badge>}
        </div>
        {submission && submission.mine !== false && (
          <Link
            href={`/problems/${submission.problemId}/solve?submission=${id}${step !== undefined ? `&step=${step}` : ''}`}
            className={styles.toolbarLink}
            aria-label="풀이 화면에서 열기"
          >
            <PenLine size={16} aria-hidden="true" />
            <span className={styles.toolbarLinkText}>풀이 화면에서 열기</span>
          </Link>
        )}
      </header>
      <div className={styles.main}>{children}</div>
    </div>
  )
}

/**
 * 키보드 (UI §5.3) — ←/→ 한 걸음, Shift+←/→ 중요 이벤트, Home/End, Space 재생.
 *
 * 화살표를 스스로 쓰는 컨트롤(입력·탭 목록·크기 조절 막대) 위에서는 비킨다. 스크러버도 그렇다 —
 * range 입력은 화살표로 한 걸음씩 움직이므로 둘 다 받으면 두 걸음 간다. Shift 와 함께일 때만 가로챈다.
 */
function useReplayKeys(actions: {
  onStep: (delta: number) => void
  onImportant: (direction: 1 | -1) => void
  onHome: () => void
  onEnd: () => void
  onToggle: () => void
}) {
  const latest = useRef(actions)
  latest.current = actions

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented) return
      const target = event.target as HTMLElement | null
      const tag = target?.tagName
      if (tag === 'TEXTAREA' || tag === 'SELECT' || target?.isContentEditable) return
      if (target?.closest('[role="tablist"], [role="separator"]')) return
      const range = tag === 'INPUT' && (target as HTMLInputElement).type === 'range'
      if (tag === 'INPUT' && !range) return
      const { onStep, onImportant, onHome, onEnd, onToggle } = latest.current

      if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
        const direction = event.key === 'ArrowRight' ? 1 : -1
        if (event.shiftKey) onImportant(direction)
        else if (range) return
        else onStep(direction)
      } else if (event.key === 'Home' || event.key === 'End') {
        if (range) return
        if (event.key === 'Home') onHome()
        else onEnd()
      } else if (event.key === ' ') {
        // 버튼·링크 위의 Space 는 그 버튼을 누르는 것이다
        if (tag === 'BUTTON' || tag === 'A' || tag === 'SUMMARY' || target?.getAttribute('role') === 'button') return
        onToggle()
      } else {
        return
      }
      event.preventDefault()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [latest])
}

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
      // 기억하지 못할 뿐 동작은 같다
    }
  },
}
