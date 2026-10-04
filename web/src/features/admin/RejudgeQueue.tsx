import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Badge, Button, InlineAlert, ProgressBar, Skeleton, TextField, Textarea } from '../../design'
import type { BadgeTone } from '../../design'
import { adminApi } from './adminApi'
import type { Me, RejudgeJob, RejudgeStatus } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'

const STATUS: Record<RejudgeStatus, { label: string; tone: BadgeTone }> = {
  REQUESTED: { label: '승인 대기', tone: 'warning' },
  APPROVED: { label: '실행 대기', tone: 'brand' },
  REJECTED: { label: '반려', tone: 'neutral' },
  RUNNING: { label: '진행 중', tone: 'brand' },
  COMPLETED: { label: '완료', tone: 'success' },
}

const REFRESH = [['admin', 'rejudges'], ['admin', 'count', 'rejudges']]

/**
 * 재채점 (§9.2, §11.2).
 *
 * 요청(JUDGE_OPERATOR) → 승인(REVIEWER, 요청자 아님) → 실행(JUDGE_OPERATOR). 승인과 실행을 나눈 것은
 * 되돌릴 수 없는 일을 클릭 한 번으로 시작하지 않기 위해서다. dry-run 은 판정을 바꾸지 않고 무엇이
 * 바뀌었을지만 본다.
 */
export function RejudgeQueue({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'rejudges'], queryFn: adminApi.rejudges, refetchInterval: 15_000 })
  const operator = me.roles.includes('JUDGE_OPERATOR')

  return (
    <>
      {operator && <RequestForm />}
      {query.isError ? (
        <InlineAlert tone="danger">{query.error.message}</InlineAlert>
      ) : !query.data ? (
        <Skeleton height={200} />
      ) : query.data.length === 0 ? (
        <Clear>재채점 작업이 없습니다.</Clear>
      ) : (
        <ul className={styles.cards}>
          {query.data.map((job) => (
            <li key={job.id}>
              <JobCard job={job} me={me} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}

function RequestForm() {
  const [scope, setScope] = useState('')
  const [reason, setReason] = useState('')
  const [dryRun, setDryRun] = useState(true)
  const request = useDecision(() => adminApi.requestRejudge(scope.trim(), reason.trim(), dryRun), '재채점을 요청했습니다 — 다른 검수자의 승인을 기다립니다', REFRESH)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    request.mutate(undefined, {
      onSuccess: () => {
        setScope('')
        setReason('')
      },
    })
  }
  return (
    <form className={styles.panelForm} onSubmit={submit} aria-label="재채점 요청">
      <h2 className={styles.subTitle}>재채점 요청</h2>
      <TextField
        label="범위"
        hint="problem:<문제 id> · submission:<제출 id> · project:<프로젝트 id>"
        value={scope}
        onChange={(event) => setScope(event.target.value)}
        className={styles.mono}
        spellCheck={false}
      />
      <Textarea label="사유" rows={2} value={reason} onChange={(event) => setReason(event.target.value)} />
      <label className={styles.inlineCheck}>
        <input type="checkbox" checked={dryRun} onChange={(event) => setDryRun(event.target.checked)} />
        dry-run — 판정을 바꾸지 않고 무엇이 바뀔지만 봅니다
      </label>
      {request.error instanceof Error && <InlineAlert tone="danger">{request.error.message}</InlineAlert>}
      <div>
        <Button type="submit" variant="primary" loading={request.isPending} disabled={!/^(problem|submission|project):.+/.test(scope.trim()) || !reason.trim()}>
          요청
        </Button>
      </div>
    </form>
  )
}

function JobCard({ job, me }: { job: RejudgeJob; me: Me }) {
  const [dialog, setDialog] = useState<'approve' | 'reject' | 'dispatch' | null>(null)
  const approve = useDecision(() => adminApi.approveRejudge(job.id), '승인했습니다 — 실행은 채점 운영자가 합니다', REFRESH)
  const reject = useDecision((reason: string) => adminApi.rejectRejudge(job.id, reason), '반려했습니다', REFRESH)
  const dispatch = useDecision(() => adminApi.dispatchRejudge(job.id), '실행했습니다', REFRESH)
  const reviewer = me.roles.includes('REVIEWER')
  const operator = me.roles.includes('JUDGE_OPERATOR')
  const ownRequest = job.requestedBy === me.userId
  const started = job.status === 'RUNNING' || job.status === 'COMPLETED'

  return (
    <article className={styles.card} aria-label={`재채점 ${job.scope}`}>
      <header className={styles.cardHead}>
        <code className={styles.cardTitle}>{job.scope}</code>
        <Badge tone={STATUS[job.status].tone}>{STATUS[job.status].label}</Badge>
        {job.dryRun && <Badge tone="trace">dry-run</Badge>}
        <span className={styles.cardMeta}>
          요청 <Id value={job.requestedBy} mine={me.userId} /> · <When iso={job.createdAt} />
          {job.approvedBy && (
            <>
              {' '}
              · 승인 <Id value={job.approvedBy} mine={me.userId} />
            </>
          )}
        </span>
      </header>
      <p className={styles.cardBody}>{job.reason}</p>
      {started && <Progress job={job} />}
      <div className={styles.cardActions}>
        {job.status === 'REQUESTED' && reviewer && (
          <>
            <Button size="dense" variant="primary" disabled={ownRequest} title={ownRequest ? '요청한 사람은 승인할 수 없습니다 (2인 원칙)' : undefined} onClick={() => setDialog('approve')}>
              승인
            </Button>
            <Button size="dense" onClick={() => setDialog('reject')}>
              반려
            </Button>
          </>
        )}
        {job.status === 'APPROVED' && operator && (
          <Button size="dense" variant={job.dryRun ? 'primary' : 'danger'} onClick={() => setDialog('dispatch')}>
            실행
          </Button>
        )}
      </div>

      {dialog === 'approve' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title="재채점 승인"
          description={`${job.scope} — 승인만으로는 아무것도 돌지 않습니다. 실행은 채점 운영자가 따로 합니다.`}
          confirm="승인"
          pending={approve.isPending}
          error={approve.error}
          onConfirm={() => approve.mutate(undefined, { onSuccess: () => setDialog(null) })}
        />
      )}
      {dialog === 'reject' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title="재채점 반려"
          confirm="반려"
          reasonLabel="반려 사유"
          pending={reject.isPending}
          error={reject.error}
          onConfirm={(reason) => reject.mutate(reason, { onSuccess: () => setDialog(null) })}
        />
      )}
      {dialog === 'dispatch' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title={job.dryRun ? 'dry-run 실행' : '재채점 실행'}
          description={
            job.dryRun
              ? '판정은 바뀌지 않습니다. 바뀌었을 판정만 보고서에 남습니다.'
              : `대상 제출 ${job.targetCount}건을 다시 채점합니다. 판정이 바뀌면 사용자에게 보이고, 되돌릴 수 없습니다.`
          }
          confirm="실행"
          danger={!job.dryRun}
          pending={dispatch.isPending}
          error={dispatch.error}
          onConfirm={() => dispatch.mutate(undefined, { onSuccess: () => setDialog(null) })}
        />
      )}
    </article>
  )
}

/** 진행과 바뀐 판정. dry-run 이면 "바뀌었을" 판정이다. */
function Progress({ job }: { job: RejudgeJob }) {
  const report = useQuery({
    queryKey: ['admin', 'rejudge', job.id],
    queryFn: () => adminApi.rejudgeReport(job.id),
    refetchInterval: job.status === 'RUNNING' ? 5_000 : false,
  })
  if (!report.data) return <Skeleton height={24} />
  const { completed, changes } = report.data
  return (
    <div className={styles.progress}>
      <ProgressBar value={completed} max={Math.max(1, job.targetCount)} label="재채점 진행" />
      <span className={styles.muted}>
        {completed}/{job.targetCount} · {job.dryRun ? '바뀌었을' : '바뀐'} 판정 {changes.length}건
      </span>
      {changes.length > 0 && (
        <details>
          <summary>바뀐 판정 보기</summary>
          <ul className={styles.changes}>
            {changes.slice(0, 50).map((change) => (
              <li key={change.submissionId}>
                <Id value={change.submissionId} /> {change.from} → <strong>{change.to}</strong>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  )
}
