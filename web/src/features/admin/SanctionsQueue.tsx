import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Badge, Button, InlineAlert, Select, Skeleton, TextField, Textarea } from '../../design'
import { adminApi } from './adminApi'
import type { Me, Sanction, SanctionKind } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'
import { date } from '../../shared/format'

const KIND_LABEL: Record<SanctionKind, string> = { WARNING: '경고', MUTE: '글쓰기 제한', SUSPEND: '정지' }

const REFRESH = [['admin', 'appeals'], ['admin', 'count', 'sanctions'], ['admin', 'sanction-history']]

/**
 * 제재·이의 (§8.5 단계적 제재, §10.4 이의 절차). SECURITY_ADMIN.
 *
 * **근거 없는 제재는 없다** — 발부에는 근거(`similarity:<id>` 나 `report:<id>`)가 필수다. 신호·신고
 * 화면의 "제재 검토로"가 그 근거와 사용자를 채워서 여기로 온다. 이의는 발부한 사람이 아닌 다른 보안
 * 관리자가 판단한다 (서버가 막는다).
 */
export function SanctionsQueue({ me }: { me: Me }) {
  const params = new URLSearchParams(window.location.search)
  const [lookup, setLookup] = useState(params.get('user') ?? '')
  return (
    <>
      <Appeals me={me} />
      <IssueForm initialUser={params.get('user') ?? ''} initialEvidence={params.get('evidence') ?? ''} onIssued={setLookup} />
      <History userId={lookup} onChange={setLookup} me={me} />
    </>
  )
}

function Appeals({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'appeals'], queryFn: adminApi.appeals })
  return (
    <section aria-labelledby="appeals-heading" className={styles.block}>
      <h2 id="appeals-heading" className={styles.subTitle}>
        열린 이의
      </h2>
      {query.isError ? (
        <InlineAlert tone="danger">{query.error.message}</InlineAlert>
      ) : !query.data ? (
        <Skeleton height={120} />
      ) : query.data.length === 0 ? (
        <Clear>판단을 기다리는 이의가 없습니다.</Clear>
      ) : (
        <ul className={styles.cards}>
          {query.data.map((sanction) => (
            <li key={sanction.id}>
              <AppealCard sanction={sanction} me={me} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function SanctionSummary({ sanction, me }: { sanction: Sanction; me: Me }) {
  return (
    <>
      <header className={styles.cardHead}>
        <Badge tone={sanction.kind === 'SUSPEND' ? 'danger' : 'warning'}>{KIND_LABEL[sanction.kind]}</Badge>
        <span className={styles.cardTitle}>{sanction.reason}</span>
        <span className={styles.cardMeta}>
          대상 <Id value={sanction.userId} /> · 발부 <Id value={sanction.issuedBy} mine={me.userId} /> · <When iso={sanction.createdAt} />
        </span>
      </header>
      <p className={styles.cardMeta}>
        근거 <code>{sanction.evidence}</code>
        {sanction.endsAt ? <> · {date(sanction.endsAt)}까지</> : ' · 기한 없음'}
        {sanction.liftedAt && ' · 해제됨'}
      </p>
    </>
  )
}

function AppealCard({ sanction, me }: { sanction: Sanction; me: Me }) {
  const [dialog, setDialog] = useState<'uphold' | 'lift' | null>(null)
  const resolve = useDecision(
    ({ uphold, note }: { uphold: boolean; note: string }) => adminApi.resolveAppeal(sanction.id, uphold, note || null),
    '이의를 판단했습니다',
    REFRESH,
  )
  const issuer = sanction.issuedBy === me.userId
  return (
    <article className={styles.card} aria-label={`이의 — ${sanction.reason}`}>
      <SanctionSummary sanction={sanction} me={me} />
      <blockquote className={styles.appeal}>
        <p>{sanction.appeal}</p>
        {sanction.appealedAt && (
          <footer className={styles.cardMeta}>
            이의 <When iso={sanction.appealedAt} />
          </footer>
        )}
      </blockquote>
      <div className={styles.cardActions}>
        <Button size="dense" variant="primary" disabled={issuer} title={issuer ? '발부한 사람은 이의를 판단할 수 없습니다' : undefined} onClick={() => setDialog('lift')}>
          받아들여 해제
        </Button>
        <Button size="dense" disabled={issuer} onClick={() => setDialog('uphold')}>
          제재 유지
        </Button>
      </div>
      {dialog && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title={dialog === 'lift' ? '이의를 받아들여 해제' : '제재 유지'}
          description="판단과 메모는 당사자에게 보입니다."
          confirm={dialog === 'lift' ? '해제' : '유지'}
          reasonLabel="당사자에게 보일 메모"
          pending={resolve.isPending}
          error={resolve.error}
          onConfirm={(note) => resolve.mutate({ uphold: dialog === 'uphold', note }, { onSuccess: () => setDialog(null) })}
        />
      )}
    </article>
  )
}

function IssueForm({ initialUser, initialEvidence, onIssued }: { initialUser: string; initialEvidence: string; onIssued: (userId: string) => void }) {
  const [userId, setUserId] = useState(initialUser)
  const [evidence, setEvidence] = useState(initialEvidence)
  const [kind, setKind] = useState<SanctionKind>('WARNING')
  const [days, setDays] = useState('')
  const [reason, setReason] = useState('')
  const issue = useDecision(
    () => adminApi.issueSanction({ userId: userId.trim(), kind, reason: reason.trim(), evidence: evidence.trim(), days: days ? Number(days) : null }),
    '제재를 발부했습니다',
    REFRESH,
  )
  const valid = userId.trim() && reason.trim() && /^(similarity|report):.+/.test(evidence.trim())
  const submit = (event: FormEvent) => {
    event.preventDefault()
    issue.mutate(undefined, {
      onSuccess: () => {
        onIssued(userId.trim())
        setReason('')
      },
    })
  }
  return (
    <form className={styles.panelForm} onSubmit={submit} aria-labelledby="issue-heading">
      <h2 id="issue-heading" className={styles.subTitle}>
        제재 발부
      </h2>
      <div className={styles.formRow}>
        <TextField label="대상 사용자 id" value={userId} onChange={(event) => setUserId(event.target.value)} className={styles.mono} spellCheck={false} />
        <TextField
          label="근거"
          hint="similarity:<신호 id> 또는 report:<신고 id>"
          value={evidence}
          onChange={(event) => setEvidence(event.target.value)}
          className={styles.mono}
          spellCheck={false}
        />
      </div>
      <div className={styles.formRow}>
        <Select label="종류" value={kind} onChange={(event) => setKind(event.target.value as SanctionKind)}>
          {(Object.keys(KIND_LABEL) as SanctionKind[]).map((value) => (
            <option key={value} value={value}>
              {KIND_LABEL[value]}
            </option>
          ))}
        </Select>
        <TextField label="기간(일)" hint="비우면 기한 없음" type="number" min={1} value={days} onChange={(event) => setDays(event.target.value)} />
      </div>
      <Textarea label="사유" hint="당사자에게 보입니다." rows={2} value={reason} onChange={(event) => setReason(event.target.value)} />
      {issue.error instanceof Error && <InlineAlert tone="danger">{issue.error.message}</InlineAlert>}
      <div>
        <Button type="submit" variant="danger" loading={issue.isPending} disabled={!valid}>
          발부
        </Button>
      </div>
    </form>
  )
}

function History({ userId, onChange, me }: { userId: string; onChange: (userId: string) => void; me: Me }) {
  const [draft, setDraft] = useState(userId)
  const query = useQuery({
    queryKey: ['admin', 'sanction-history', userId],
    queryFn: () => adminApi.sanctionHistory(userId),
    enabled: userId.length > 0,
  })
  const lift = useDecision((id: string) => adminApi.liftSanction(id), '제재를 해제했습니다', REFRESH)
  const [target, setTarget] = useState<Sanction | null>(null)

  return (
    <section aria-labelledby="history-heading" className={styles.block}>
      <h2 id="history-heading" className={styles.subTitle}>
        제재 이력
      </h2>
      <form
        className={styles.formRow}
        onSubmit={(event) => {
          event.preventDefault()
          onChange(draft.trim())
        }}
      >
        <TextField label="사용자 id" value={draft} onChange={(event) => setDraft(event.target.value)} className={styles.mono} spellCheck={false} />
        <div className={styles.formButton}>
          <Button type="submit">조회</Button>
        </div>
      </form>
      {userId &&
        (query.isError ? (
          <InlineAlert tone="danger">{query.error.message}</InlineAlert>
        ) : !query.data ? (
          <Skeleton height={80} />
        ) : query.data.length === 0 ? (
          <Clear>이 사용자에게 제재 이력이 없습니다.</Clear>
        ) : (
          <ul className={styles.cards}>
            {query.data.map((sanction) => (
              <li key={sanction.id}>
                <article className={styles.card} aria-label={`제재 — ${sanction.reason}`}>
                  <SanctionSummary sanction={sanction} me={me} />
                  {!sanction.liftedAt && (
                    <div className={styles.cardActions}>
                      <Button size="dense" onClick={() => setTarget(sanction)}>
                        해제
                      </Button>
                    </div>
                  )}
                </article>
              </li>
            ))}
          </ul>
        ))}
      {target && (
        <DecisionDialog
          open
          onClose={() => setTarget(null)}
          title="제재 해제"
          description={`${KIND_LABEL[target.kind]} — ${target.reason}`}
          confirm="해제"
          pending={lift.isPending}
          error={lift.error}
          onConfirm={() => lift.mutate(target.id, { onSuccess: () => setTarget(null) })}
        />
      )}
    </section>
  )
}
