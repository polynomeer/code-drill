import { useQuery } from '@tanstack/react-query'
import { BookOpen, Check, Clapperboard, Copy, CornerDownRight, RotateCcw } from 'lucide-react'
import { Link } from 'wouter'
import { getPrescription } from '../../api/client'
import { Button, InlineAlert, ProgressBar, VerdictBadge, useToast } from '../../design'
import { IN_FLIGHT, VERDICT_LABEL } from '../../shared/types'
import type { CaseResult, GroupResult, Problem, Submission, SubmissionStatus, Verdict } from '../../shared/types'
import { DonatePanel } from '../arena/DonatePanel'
import { SharePanel } from '../discussion/SharePanel'
import { CounterexamplePanel } from './CounterexamplePanel'
import { parseCompileLog } from './compileLog'
import { diffValues } from './outputDiff'
import type { Segment } from './outputDiff'
import { decodeWire, returnKind } from './wire'
import styles from './VerdictPanel.module.css'

/**
 * 판정 결과 (UI 디자인 문서 §4.3 Result Drawer 상태, §7.2 — docs/ui-overhaul.md §6.3).
 *
 * 판정마다 1차 정보가 다르다.
 * - 진행 중: 지금 어느 단계인지. 시간 약속은 하지 않는다 (§9.2).
 * - 맞았습니다: 통과한 그룹과 점수, 그리고 **다음에 무엇을 할지** (디자인 설계서 §10.3).
 * - 틀렸습니다·런타임 오류: 처음 떨어진 공개 케이스의 입력·기댓값·실행값 (§4.2). 리플레이는 그 다음.
 * - 시간·메모리 초과: 떨어진 그룹과 제한.
 * - 컴파일 오류: 오류 줄로 가는 버튼.
 * - 시스템 오류: 코드 탓이 아님을 먼저 말하고 다시 시도와 신고 ID (디자인 설계서 §7.4).
 *
 * 상태와 판정은 색만으로 말하지 않는다 — 아이콘과 글이 함께 간다.
 */
export function VerdictPanel({
  submission,
  problem,
  hasTrace = false,
  onJumpToLine,
  onOpenReplay,
  onOpenEditorial,
  onResubmit,
}: {
  submission: Submission
  /** 공개 예제의 입력·기댓값과 제한을 보이려면 필요하다. 없으면 그 부분만 빠진다 */
  problem: Problem | null
  hasTrace?: boolean
  /** 컴파일 오류 줄로. 편집기가 있는 화면에서만 준다 */
  onJumpToLine?: (line: number, column: number | null) => void
  onOpenReplay?: () => void
  onOpenEditorial?: () => void
  /** 시스템 오류일 때 같은 코드로 다시 제출한다 */
  onResubmit?: () => void
}) {
  const inFlight = IN_FLIGHT.has(submission.status)
  const verdict = submission.verdict
  const mine = submission.mine !== false

  return (
    <section className={styles.panel} aria-label="판정">
      {inFlight ? (
        <JudgeProgress status={submission.status} />
      ) : (
        <header className={styles.head}>
          {/* 판정은 배지 하나로 — 아이콘·글·색이 함께 간다. 같은 글을 제목으로 또 쓰지 않는다 */}
          {verdict ? <VerdictBadge verdict={verdict} /> : <span>판정을 받지 못했습니다</span>}
          {submission.score !== null && verdict !== 'SYSTEM_ERROR' && (
            <p className={styles.headline}>{submission.score}점</p>
          )}
          {(submission.revision ?? 1) > 1 && (
            <span className={styles.meta}>재채점 {submission.revision! - 1}회를 거친 판정입니다</span>
          )}
        </header>
      )}

      {!inFlight && verdict === 'SYSTEM_ERROR' && <SystemError submission={submission} onResubmit={onResubmit} />}

      {!inFlight && verdict === 'COMPILE_ERROR' && (
        <CompileErrorView log={submission.compileLog ?? ''} onJumpToLine={onJumpToLine} />
      )}

      {!inFlight && verdict && FAILED_RUN.has(verdict) && (
        <FailureView submission={submission} problem={problem} hasTrace={hasTrace} onOpenReplay={onOpenReplay} />
      )}

      {!inFlight && verdict === 'ACCEPTED' && mine && (
        <AcceptedNext
          problemId={submission.problemId}
          hasTrace={hasTrace}
          onOpenReplay={onOpenReplay}
          onOpenEditorial={onOpenEditorial}
        />
      )}

      {submission.groups && submission.groups.length > 0 && <GroupTable groups={submission.groups} />}

      {/* 다음 행동 — 반례 줄이기와 내놓기는 소유자의 일이다 (§8.3, §8.5) */}
      {!inFlight && (
        <div className={styles.more}>
          {mine && verdict && FAILED_RUN.has(verdict) && <CounterexamplePanel submissionId={submission.id} />}
          {mine && <DonatePanel submission={submission} />}
          <SharePanel submission={submission} />
        </div>
      )}
    </section>
  )
}

/** 실행까지 갔다가 떨어진 판정 — 입력·출력을 견줄 수 있는 것들 */
const FAILED_RUN = new Set<Verdict>(['WRONG_ANSWER', 'RUNTIME_ERROR', 'TIME_LIMIT', 'MEMORY_LIMIT', 'OUTPUT_LIMIT'])

/* ─── 진행 ─── */

const STEPS: { label: string; statuses: SubmissionStatus[]; text: string }[] = [
  { label: '대기', statuses: ['CREATED', 'QUEUED', 'LEASED'], text: '채점을 기다리고 있습니다.' },
  { label: '컴파일', statuses: ['COMPILING'], text: '코드를 컴파일하고 있습니다.' },
  { label: '실행', statuses: ['RUNNING'], text: '테스트를 실행하고 있습니다.' },
  { label: '정리', statuses: ['AGGREGATING'], text: '결과를 정리하고 있습니다.' },
]

/** 진행 단계 (디자인 설계서 §7.1). 문구는 지금 하는 일만 말하고 남은 시간을 약속하지 않는다. */
export function JudgeProgress({ status }: { status: SubmissionStatus }) {
  const current = Math.max(0, STEPS.findIndex((step) => step.statuses.includes(status)))
  return (
    <div className={styles.progress}>
      <ol className={styles.steps} aria-label="채점 단계">
        {STEPS.map((step, index) => (
          <li
            key={step.label}
            className={index < current ? styles.stepDone : index === current ? styles.stepNow : styles.stepNext}
            aria-current={index === current ? 'step' : undefined}
          >
            <span className={styles.dot} aria-hidden="true">
              {index < current ? <Check size={12} /> : index + 1}
            </span>
            {step.label}
          </li>
        ))}
      </ol>
      <p className={styles.progressText} role="status">
        {STEPS[current]!.text}
      </p>
    </div>
  )
}

/* ─── 떨어진 실행 ─── */

function FailureView({
  submission,
  problem,
  hasTrace,
  onOpenReplay,
}: {
  submission: Submission
  problem: Problem | null
  hasTrace: boolean
  onOpenReplay?: () => void
}) {
  const groups = submission.groups ?? []
  const failure = firstFailure(groups)
  const failedGroups = groups.filter((group) => group.verdict !== 'ACCEPTED')
  const sample = failure && problem?.samples.find((item) => item.id === failure.caseId)
  const verdict = submission.verdict!

  return (
    <div className={styles.failure}>
      {failure && sample ? (
        <CaseDiff
          title={`예제 ${failure.caseId} 에서 ${VERDICT_LABEL[failure.verdict]}`}
          args={sample.args}
          expected={sample.expected}
          result={failure}
          signature={problem!.signature}
        />
      ) : (
        // 공개 예제는 다 통과했고 숨은 그룹에서 떨어졌다. 입력은 보이지 않는다 (§8.3).
        failedGroups.length > 0 && (
          <p className={styles.note}>
            {failedGroups.map((group) => (
              <code key={group.groupId} className={styles.groupName}>
                {group.groupId}
              </code>
            ))}
            그룹에서 {VERDICT_LABEL[verdict]}. 숨은 그룹이라 입력은 공개하지 않습니다 — 아래 "가장 작은 반례
            찾기"로 떨어지는 작은 입력을 찾을 수 있습니다.
          </p>
        )
      )}

      {(verdict === 'TIME_LIMIT' || verdict === 'MEMORY_LIMIT') && problem && (
        <p className={styles.limits}>
          제한 {problem.timeMillis % 1000 === 0 ? `${problem.timeMillis / 1000}초` : `${problem.timeMillis}ms`} ·{' '}
          {problem.memoryMb}MB.{' '}
          {verdict === 'TIME_LIMIT'
            ? '입력이 커질 때 반복이 몇 번 도는지 세어 보세요 — 같은 일을 되풀이하는 곳이 병목입니다.'
            : '입력 크기에 비례해 쌓이는 자료구조가 있는지 보세요.'}
        </p>
      )}

      {hasTrace && onOpenReplay && (
        <Button icon={<Clapperboard size={16} />} onClick={onOpenReplay}>
          최초 분기점 보기
        </Button>
      )}
    </div>
  )
}

/** 처음 떨어진 케이스. 그룹 순서(공개 먼저)를 따른다. 숨은 그룹은 케이스가 비어 와서 건너뛴다. */
function firstFailure(groups: GroupResult[]): CaseResult | null {
  for (const group of groups) {
    const failed = group.cases.find((item) => item.verdict !== 'ACCEPTED')
    if (failed) return failed
  }
  return null
}

/** 입력·기댓값·실행값 (UI 디자인 문서 §7.3 Diff Block) */
function CaseDiff({
  title,
  args,
  expected,
  result,
  signature,
}: {
  title: string
  args: unknown[]
  expected: unknown
  result: CaseResult
  signature: string
}) {
  const decoded = result.actual == null ? null : decodeWire(returnKind(signature), result.actual)
  const view =
    decoded === null
      ? null
      : decoded.ok
        ? diffValues(expected, decoded.value)
        : diffValues(expected, undefined, decoded.raw)

  return (
    <div className={styles.diff}>
      <p className={styles.diffTitle}>{title}</p>
      <dl className={styles.diffRows}>
        <div>
          <dt>입력</dt>
          <dd>
            <code>{args.map((arg) => JSON.stringify(arg)).join(', ')}</code>
          </dd>
        </div>
        <div>
          <dt>기댓값</dt>
          <dd>{view ? <Segments segments={view.expected} /> : <code>{JSON.stringify(expected)}</code>}</dd>
        </div>
        <div>
          <dt>실행값</dt>
          <dd>
            {view ? (
              <Segments segments={view.actual} />
            ) : (
              <span className={styles.meta}>
                {result.message ? <code>{result.message}</code> : '값을 내기 전에 멈췄습니다'}
              </span>
            )}
          </dd>
        </div>
      </dl>
      {view?.lengths && (
        <p className={styles.meta}>
          길이가 다릅니다 — 기댓값 {view.lengths.expected}개, 실행값 {view.lengths.actual}개
        </p>
      )}
      {view && view.at !== null && (
        <p className={styles.meta}>
          <CornerDownRight size={12} aria-hidden="true" /> {view.at + 1}번째 글자부터 다릅니다
        </p>
      )}
    </div>
  )
}

function Segments({ segments }: { segments: Segment[] }) {
  return (
    <code className={styles.segments}>
      {segments.map((segment, index) =>
        segment.differs ? (
          <mark key={index} className={styles.mark}>
            {segment.text}
          </mark>
        ) : (
          <span key={index}>{segment.text}</span>
        ),
      )}
    </code>
  )
}

/* ─── 컴파일 오류 ─── */

function CompileErrorView({
  log,
  onJumpToLine,
}: {
  log: string
  onJumpToLine?: (line: number, column: number | null) => void
}) {
  const errors = parseCompileLog(log)
  return (
    <div className={styles.compile}>
      {errors.length > 0 && (
        <ul className={styles.compileList}>
          {errors.map((error) => (
            <li key={error.line}>
              {onJumpToLine ? (
                <button type="button" className={styles.jump} onClick={() => onJumpToLine(error.line, error.column)}>
                  {error.line}번 줄{error.column !== null && ` ${error.column}열`}로 이동
                </button>
              ) : (
                <span className={styles.lineRef}>
                  {error.line}번 줄{error.column !== null && ` ${error.column}열`}
                </span>
              )}
              <span>{error.message}</span>
            </li>
          ))}
        </ul>
      )}
      <details open={errors.length === 0} className={styles.log}>
        <summary>컴파일러 메시지 전체</summary>
        <pre tabIndex={0}>{log}</pre>
      </details>
    </div>
  )
}

/* ─── 시스템 오류 (디자인 설계서 §7.4) ─── */

function SystemError({ submission, onResubmit }: { submission: Submission; onResubmit?: () => void }) {
  const toast = useToast()
  return (
    <InlineAlert
      tone="warning"
      title="플랫폼 오류로 채점하지 못했습니다"
      action={
        onResubmit && (
          <Button size="dense" icon={<RotateCcw size={14} />} onClick={onResubmit}>
            다시 제출
          </Button>
        )
      }
    >
      <p>코드 문제가 아닙니다. 다시 제출해 보고, 계속되면 아래 신고 ID 를 알려 주세요.</p>
      <p className={styles.reportId}>
        신고 ID <code>{submission.id}</code>
        <button
          type="button"
          className={styles.copy}
          aria-label="신고 ID 복사"
          onClick={() =>
            void navigator.clipboard
              ?.writeText(submission.id)
              .then(() => toast.show('신고 ID 를 복사했습니다', 'success'))
          }
        >
          <Copy size={14} aria-hidden="true" />
        </button>
      </p>
    </InlineAlert>
  )
}

/* ─── 맞았습니다 — 다음 행동 (디자인 설계서 §10.3) ─── */

function AcceptedNext({
  problemId,
  hasTrace,
  onOpenReplay,
  onOpenEditorial,
}: {
  problemId: string
  hasTrace: boolean
  onOpenReplay?: () => void
  onOpenEditorial?: () => void
}) {
  const prescription = useQuery({ queryKey: ['prescription'], queryFn: getPrescription, staleTime: 60_000 })
  const next = prescription.data?.items.find((item) => item.problemId !== problemId)

  return (
    <div className={styles.next}>
      <p className={styles.nextTitle}>다음에 할 것</p>
      <div className={styles.nextActions}>
        {hasTrace && onOpenReplay && (
          <Button size="dense" icon={<Clapperboard size={14} />} onClick={onOpenReplay}>
            내 실행 리플레이
          </Button>
        )}
        {onOpenEditorial && (
          <Button size="dense" icon={<BookOpen size={14} />} onClick={onOpenEditorial}>
            해설과 다른 풀이
          </Button>
        )}
        {next && (
          <Link href={`/problems/${next.problemId}/solve`} className={styles.nextLink}>
            처방의 다음 문제 <code>{next.problemId}</code>
          </Link>
        )}
      </div>
      <p className={styles.meta}>맞힌 문제는 잊을 즈음 오늘의 처방이 복습으로 다시 꺼냅니다.</p>
    </div>
  )
}

/* ─── 그룹 ─── */

function GroupTable({ groups }: { groups: GroupResult[] }) {
  return (
    <table className={styles.groups}>
      <caption className={styles.caption}>그룹별 결과</caption>
      <thead>
        <tr>
          <th scope="col">그룹</th>
          <th scope="col">판정</th>
          <th scope="col">점수</th>
          <th scope="col">
            <span className="visually-hidden">비율</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {groups.map((group) => (
          <tr key={group.groupId}>
            <td>
              <code>{group.groupId}</code>
            </td>
            <td>
              <VerdictBadge verdict={group.verdict} />
            </td>
            <td className={styles.num}>{group.maxScore > 0 ? `${group.score} / ${group.maxScore}` : '예제'}</td>
            <td className={styles.bar}>
              {group.maxScore > 0 && (
                <ProgressBar
                  label={`${group.groupId} 그룹 점수`}
                  value={group.score}
                  max={group.maxScore}
                  tone={group.score === group.maxScore ? 'success' : group.score > 0 ? 'warning' : 'danger'}
                />
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
