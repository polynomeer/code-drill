import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, Flame, Lightbulb, Play, Repeat, Shuffle } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'wouter'
import {
  closeCoaching,
  deferPrescribed,
  explainTransfer,
  getOnboarding,
  getPrescription,
  listOpenCoaching,
  listOpenTransfers,
  skipPrescribed,
} from '../../api/client'
import { Badge, Button, DifficultyBadge, EmptyState, InlineAlert, Skeleton, Textarea, useToast } from '../../design'
import { COMPETENCY_LABEL, REASON_LABEL } from '../../shared/types'
import type { CoachingSession, PrescribedProblem, Prescription, TransferTask } from '../../shared/types'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import styles from './TrainingPage.module.css'
import { monthDay } from '../../shared/format'

/**
 * 훈련 `/training` (UI 디자인 문서 §9.4 Daily prescription, docs/ui-overhaul.md §6.5).
 *
 * > 오늘 집중할 1~2개 역량과 추천 이유 — 시작·교체·미루기.
 *
 * 처방 카드가 화면의 주인이다. 이유가 문제 이름보다 먼저·크게 보인다 — "왜 이걸 풀어야 하나"에
 * 답하지 못하는 추천은 건너뛰어진다. 아래에 하던 것(열어 둔 코칭, 걸려 있는 전이 과제)을 둔다.
 *
 * 교체와 미루기는 둘 다 오늘의 자리를 비우고 다음 후보로 채운다. 다른 것은 그 문제가 돌아오는
 * 때다 — 교체는 내일, 미루기는 사흘 뒤 (LearningService.defer).
 */
export function TrainingPage() {
  const query = useQuery({ queryKey: ['me', 'prescription'], queryFn: getPrescription })
  const prescription = query.data
  const onboarding = useQuery({ queryKey: ['me', 'onboarding'], queryFn: getOnboarding })

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <div>
          <h1 className={styles.title}>오늘의 훈련</h1>
          {prescription && <StreakLine prescription={prescription} />}
        </div>
        <Link href="/competencies" className={styles.headLink}>
          역량 지도 보기
        </Link>
      </header>

      {/* 세 문항에 답하지 않았으면 진단 없이 "가장 쉬운 문제"만 권한다 — 진단으로 시작하자고 권한다 */}
      {onboarding.data === null && (
        <InlineAlert
          tone="info"
          title="세 가지만 답하면 진단으로 시작합니다"
          action={
            <Link href="/welcome?next=/training" className={styles.headLink}>
              답하기
            </Link>
          }
        >
          하루 목표·주 언어·지금 수준에 맞춰 역량군마다 첫 근거를 만들 문제를 고릅니다.
        </InlineAlert>
      )}

      {query.isError ? (
        <InlineAlert tone="danger" title="처방을 불러오지 못했습니다" action={<Button size="dense" onClick={() => void query.refetch()}>다시 시도</Button>} />
      ) : !prescription ? (
        <div className={styles.featured} role="status" aria-busy="true">
          <span className="visually-hidden">불러오는 중</span>
          <Skeleton height={220} />
          <Skeleton height={220} />
        </div>
      ) : (
        <Prescribed prescription={prescription} />
      )}

      <InProgress />
    </div>
  )
}

/**
 * 연속 일수. 숫자 하나로 충분하다 — 다만 어제를 빠뜨렸으면 오늘이 위험하다고 **미리** 말한다.
 * 끊긴 뒤에 알려 주는 것은 알려 주는 것이 아니다.
 */
function StreakLine({ prescription }: { prescription: Prescription }) {
  const { streak } = prescription
  return (
    <p className={styles.streak}>
      <Flame size={16} aria-hidden="true" />
      {streak.days > 0 ? `${streak.days}일째 이어 가는 중` : '아직 시작 전'}
      {streak.activeToday && <Badge tone="success">오늘 함</Badge>}
      {streak.atRisk && !streak.activeToday && <Badge tone="warning">오늘 안 하면 끊깁니다</Badge>}
    </p>
  )
}

/** 크게 보일 카드 수 (UI §9.4 "1~2개"). 나머지는 후보 목록으로 접는다. */
const FEATURED = 2

function Prescribed({ prescription }: { prescription: Prescription }) {
  const client = useQueryClient()
  const toast = useToast()
  const index = useProblemIndex()

  const adjust = useMutation({
    mutationFn: ({ item, how }: { item: PrescribedProblem; how: 'swap' | 'defer' }) =>
      how === 'swap' ? skipPrescribed(item.problemId) : deferPrescribed(item.problemId),
    onSuccess: (next, { item, how }) => {
      client.setQueryData(['me', 'prescription'], next)
      const name = problemLabel(index, item.problemId)
      toast.show(how === 'swap' ? `${name} 대신 다른 문제를 골랐습니다` : `${name}은(는) 사흘 뒤에 다시 권합니다`, 'success')
    },
    onError: () => toast.show('처방을 바꾸지 못했습니다. 잠시 뒤 다시 시도하세요.', 'danger'),
  })

  if (prescription.items.length === 0) {
    return (
      <EmptyState
        title="오늘 권할 것이 없습니다"
        action={
          <Link href="/problems" className="linklike">
            문제 고르기
          </Link>
        }
      >
        약점·재발·복습 어느 것에도 걸린 문제가 없습니다. 아무 문제나 풀어도 기록이 쌓입니다.
      </EmptyState>
    )
  }

  const featured = prescription.items.slice(0, FEATURED)
  const rest = prescription.items.slice(FEATURED)

  return (
    <>
      <section aria-labelledby="today-heading">
        <h2 id="today-heading" className={styles.sectionTitle}>
          오늘 집중할 것
        </h2>
        <ul className={styles.featured}>
          {featured.map((item) => (
            <li key={item.problemId}>
              <PrescriptionCard
                item={item}
                label={problemLabel(index, item.problemId)}
                difficulty={index.get(item.problemId)?.difficulty}
                busy={adjust.isPending && adjust.variables?.item.problemId === item.problemId}
                onSwap={() => adjust.mutate({ item, how: 'swap' })}
                onDefer={() => adjust.mutate({ item, how: 'defer' })}
              />
            </li>
          ))}
        </ul>
      </section>

      {rest.length > 0 && (
        <section aria-labelledby="more-heading">
          <h2 id="more-heading" className={styles.sectionTitle}>
            그 밖의 후보
          </h2>
          <ul className={styles.rest}>
            {rest.map((item) => (
              <li key={item.problemId}>
                <Badge>{REASON_LABEL[item.reason]}</Badge>
                <Link href={`/problems/${item.problemId}/solve`} className={styles.restLink}>
                  {problemLabel(index, item.problemId)}
                </Link>
                <span className={styles.muted}>{item.detail}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}

function PrescriptionCard({
  item,
  label,
  difficulty,
  busy,
  onSwap,
  onDefer,
}: {
  item: PrescribedProblem
  label: string
  difficulty: Parameters<typeof DifficultyBadge>[0]['level'] | undefined
  busy: boolean
  onSwap: () => void
  onDefer: () => void
}) {
  const competency = item.competency ? (COMPETENCY_LABEL[item.competency] ?? item.competency) : null
  return (
    <article className={styles.card} aria-labelledby={`rx-${item.problemId}`}>
      <div className={styles.cardTop}>
        <Badge tone="brand">{REASON_LABEL[item.reason]}</Badge>
        {competency && <span className={styles.competency}>{competency}</span>}
      </div>
      {/* 이유가 문제 이름보다 먼저다 */}
      <p className={styles.reason}>{item.detail}</p>
      <h3 id={`rx-${item.problemId}`} className={styles.problem}>
        {label}
        {difficulty && <DifficultyBadge level={difficulty} />}
      </h3>
      <p className={styles.measure}>
        <CalendarClock size={14} aria-hidden="true" />
        다음 측정 {monthDay(item.nextMeasurement)}
      </p>
      <div className={styles.actions}>
        <Link href={`/problems/${item.problemId}/solve`} className={styles.start}>
          <Play size={16} aria-hidden="true" />
          시작
        </Link>
        <Button icon={<Shuffle size={16} />} onClick={onSwap} disabled={busy} aria-label={`${label} 교체 — 오늘은 다른 문제로`}>
          교체
        </Button>
        <Button variant="tertiary" icon={<Repeat size={16} />} onClick={onDefer} disabled={busy} aria-label={`${label} 미루기 — 사흘 뒤에 다시`}>
          미루기
        </Button>
      </div>
    </article>
  )
}

/* ─── 하던 것 ─── */

/**
 * 열어 둔 코칭과 걸려 있는 전이 과제 (ui-overhaul.md §6.5 "아래에 진행 중인 코칭 세션과 전이 과제").
 *
 * 전이 과제가 먼저다. 설명을 쓰고 변형 문제를 힌트 없이 풀면 가장 무거운 증거가 된다 (FR-807) —
 * 하던 것 중 가장 값이 큰 일이다.
 */
function InProgress() {
  const sessions = useQuery({ queryKey: ['coaching', 'open'], queryFn: listOpenCoaching })
  const transfers = useQuery({ queryKey: ['coaching', 'transfers'], queryFn: listOpenTransfers })
  const index = useProblemIndex()

  if (sessions.isPending || transfers.isPending) return null
  const openSessions = sessions.data ?? []
  const openTransfers = transfers.data ?? []

  return (
    <section aria-labelledby="progress-heading" className={styles.progressSection}>
      <h2 id="progress-heading" className={styles.sectionTitle}>
        하던 것
      </h2>
      {(sessions.isError || transfers.isError) && (
        <InlineAlert tone="warning">하던 것 일부를 불러오지 못했습니다.</InlineAlert>
      )}
      {openSessions.length === 0 && openTransfers.length === 0 && !sessions.isError && !transfers.isError ? (
        <p className={styles.muted}>
          열어 둔 코칭도, 걸려 있는 전이 과제도 없습니다. 풀이 화면에서 막혔을 때 "코칭"을 열면 여기 남습니다.
        </p>
      ) : (
        <ul className={styles.progressList}>
          {openTransfers.map((task) => (
            <li key={task.id}>
              <TransferCard task={task} index={index} />
            </li>
          ))}
          {openSessions.map((session) => (
            <li key={session.id}>
              <SessionCard session={session} label={problemLabel(index, session.problemId)} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function TransferCard({ task, index }: { task: TransferTask; index: ReturnType<typeof useProblemIndex> }) {
  const client = useQueryClient()
  const [text, setText] = useState('')
  const explain = useMutation({
    mutationFn: () => explainTransfer(task.id, text),
    onSuccess: (next) =>
      client.setQueryData<TransferTask[]>(['coaching', 'transfers'], (all) =>
        (all ?? []).map((item) => (item.id === next.id ? next : item)),
      ),
  })
  const source = problemLabel(index, task.sourceProblemId)
  const target = problemLabel(index, task.targetProblemId)

  return (
    <article className={styles.item} aria-label={`전이 확인 — ${source}에서 ${target}로`}>
      <div className={styles.itemHead}>
        <Badge tone="trace" icon={<Lightbulb size={12} />}>
          전이 확인
        </Badge>
        <span className={styles.muted}>
          {source} 에서 배운 것을 {target} 에서
        </span>
      </div>
      {task.status === 'ASSIGNED' ? (
        <form
          className={styles.explain}
          onSubmit={(event) => {
            event.preventDefault()
            explain.mutate()
          }}
        >
          <Textarea
            label="먼저 자기 말로 — 무엇을 알게 됐나요?"
            hint="코드가 아니라 생각을 적습니다. 적고 나서 변형 문제를 힌트 없이 풀면 전이로 인정됩니다."
            rows={3}
            value={text}
            onChange={(event) => setText(event.target.value)}
            error={explain.error instanceof Error ? explain.error.message : undefined}
          />
          <Button type="submit" variant="primary" size="dense" loading={explain.isPending} disabled={text.trim().length === 0}>
            설명 내기
          </Button>
        </form>
      ) : (
        <div className={styles.itemActions}>
          <span className={styles.muted}>설명을 냈습니다. 이제 힌트 없이 풀면 가장 무거운 증거가 됩니다.</span>
          <Link href={`/problems/${task.targetProblemId}/solve`} className={styles.start}>
            <Play size={16} aria-hidden="true" />
            변형 문제 풀기
          </Link>
        </div>
      )}
    </article>
  )
}

function SessionCard({ session, label }: { session: CoachingSession; label: string }) {
  const client = useQueryClient()
  const close = useMutation({
    mutationFn: () => closeCoaching(session.id),
    onSuccess: () =>
      client.setQueryData<CoachingSession[]>(['coaching', 'open'], (all) => (all ?? []).filter((item) => item.id !== session.id)),
  })
  const focus = session.focus.map((f) => COMPETENCY_LABEL[f.competency] ?? f.competency).join(' · ')

  return (
    <article className={styles.item} aria-label={`코칭 — ${label}`}>
      <div className={styles.itemHead}>
        <Badge>코칭</Badge>
        <span className={styles.itemTitle}>{label}</span>
      </div>
      <p className={styles.muted}>
        {focus ? `${focus} · ` : ''}
        {/* 받은 도움을 감추지 않는다 (FR-806) */}
        {session.helpLevel === 0 ? '아직 힌트를 보지 않았습니다' : `${session.helpLevel}단계까지 봤습니다`}
      </p>
      <div className={styles.itemActions}>
        <Link href={`/problems/${session.problemId}/solve?coaching=1`} className={styles.secondaryLink}>
          이어 하기
        </Link>
        {/* 끝내도 받은 도움의 기록은 남는다 — 증거의 무게는 그대로다 */}
        <Button variant="tertiary" size="dense" loading={close.isPending} onClick={() => close.mutate()}>
          끝내기
        </Button>
      </div>
    </article>
  )
}
