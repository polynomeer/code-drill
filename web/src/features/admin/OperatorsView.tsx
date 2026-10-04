import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Badge, Button, InlineAlert, Select, Skeleton, TextField, Textarea } from '../../design'
import { ROLE_LABEL, adminApi } from './adminApi'
import type { AdminRole, GrantRequest, Me, RoleGrant } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'

const ROLES = Object.keys(ROLE_LABEL) as AdminRole[]
const REFRESH = [['admin', 'operators'], ['admin', 'role-requests'], ['admin', 'count', 'operators']]

/**
 * 운영자 역할 (§11.2). SECURITY_ADMIN.
 *
 * 역할 부여는 요청과 승인이 따로다 — 요청만으로는 권한이 늘지 않는다. 승인자는 요청자와도, 받는 사람과도
 * 달라야 한다 (서버가 막는다). 부트스트랩(보안 관리자가 한 명뿐)일 때만 요청이 그 자리에서 부여된다.
 */
export function OperatorsView({ me }: { me: Me }) {
  return (
    <>
      <Requests me={me} />
      <RequestForm />
      <Grants me={me} />
    </>
  )
}

function Requests({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'role-requests'], queryFn: adminApi.roleRequests })
  return (
    <section aria-labelledby="requests-heading" className={styles.block}>
      <h2 id="requests-heading" className={styles.subTitle}>
        승인을 기다리는 요청
      </h2>
      {query.isError ? (
        <InlineAlert tone="danger">{query.error.message}</InlineAlert>
      ) : !query.data ? (
        <Skeleton height={80} />
      ) : query.data.length === 0 ? (
        <Clear>승인을 기다리는 역할 요청이 없습니다.</Clear>
      ) : (
        <ul className={styles.cards}>
          {query.data.map((request) => (
            <li key={request.id}>
              <RequestCard request={request} me={me} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function RequestCard({ request, me }: { request: GrantRequest; me: Me }) {
  const [dialog, setDialog] = useState<'approve' | 'reject' | null>(null)
  const approve = useDecision(() => adminApi.approveRole(request.id), '역할을 부여했습니다', REFRESH)
  const reject = useDecision((reason: string) => adminApi.rejectRole(request.id, reason), '요청을 반려했습니다', REFRESH)
  const conflicted = request.requestedBy === me.userId || request.userId === me.userId
  return (
    <article className={styles.card} aria-label={`역할 요청 — ${ROLE_LABEL[request.role]}`}>
      <header className={styles.cardHead}>
        <Badge tone="brand">{ROLE_LABEL[request.role]}</Badge>
        <span className={styles.cardMeta}>
          받는 사람 <Id value={request.userId} mine={me.userId} /> · 요청 <Id value={request.requestedBy} mine={me.userId} /> · <When iso={request.createdAt} />
        </span>
      </header>
      <p className={styles.cardBody}>{request.reason}</p>
      <div className={styles.cardActions}>
        <Button
          size="dense"
          variant="primary"
          disabled={conflicted}
          title={conflicted ? '요청한 사람이나 받는 사람은 승인할 수 없습니다 (2인 원칙)' : undefined}
          onClick={() => setDialog('approve')}
        >
          승인
        </Button>
        <Button size="dense" onClick={() => setDialog('reject')}>
          반려
        </Button>
      </div>
      {dialog === 'approve' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title="역할 부여 승인"
          description={`승인이 곧 부여입니다 — 이 계정이 바로 ${ROLE_LABEL[request.role]} 역할을 갖습니다.`}
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
          title="역할 요청 반려"
          confirm="반려"
          reasonLabel="반려 사유"
          pending={reject.isPending}
          error={reject.error}
          onConfirm={(reason) => reject.mutate(reason, { onSuccess: () => setDialog(null) })}
        />
      )}
    </article>
  )
}

function RequestForm() {
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState<AdminRole>('REVIEWER')
  const [reason, setReason] = useState('')
  const [granted, setGranted] = useState<string | null>(null)
  const request = useDecision(() => adminApi.requestRole(userId.trim(), role, reason.trim()), '역할 부여를 요청했습니다', REFRESH)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    request.mutate(undefined, {
      onSuccess: (result) => {
        const status = (result as { status?: string } | null)?.status
        setGranted(status === 'GRANTED' ? '부트스트랩 구간이라 바로 부여됐습니다.' : status === 'UNCHANGED' ? '이미 가진 역할입니다.' : '다른 보안 관리자의 승인을 기다립니다.')
        setReason('')
      },
    })
  }
  return (
    <form className={styles.panelForm} onSubmit={submit} aria-labelledby="request-role-heading">
      <h2 id="request-role-heading" className={styles.subTitle}>
        역할 부여 요청
      </h2>
      <div className={styles.formRow}>
        <TextField label="사용자 id" value={userId} onChange={(event) => setUserId(event.target.value)} className={styles.mono} spellCheck={false} />
        <Select label="역할" value={role} onChange={(event) => setRole(event.target.value as AdminRole)}>
          {ROLES.map((value) => (
            <option key={value} value={value}>
              {ROLE_LABEL[value]} ({value})
            </option>
          ))}
        </Select>
      </div>
      <Textarea label="사유" hint="승인자가 판단할 근거입니다. 감사 로그에 남습니다." rows={2} value={reason} onChange={(event) => setReason(event.target.value)} />
      {request.error instanceof Error && <InlineAlert tone="danger">{request.error.message}</InlineAlert>}
      {granted && <InlineAlert tone="info">{granted}</InlineAlert>}
      <div>
        <Button type="submit" variant="primary" loading={request.isPending} disabled={!userId.trim() || !reason.trim()}>
          요청
        </Button>
      </div>
    </form>
  )
}

function Grants({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'operators'], queryFn: adminApi.operators })
  const [target, setTarget] = useState<RoleGrant | null>(null)
  const revoke = useDecision((grant: RoleGrant) => adminApi.revokeRole(grant.userId, grant.role), '역할을 회수했습니다', REFRESH)
  return (
    <section aria-labelledby="grants-heading" className={styles.block}>
      <h2 id="grants-heading" className={styles.subTitle}>
        지금 역할
      </h2>
      {query.isError ? (
        <InlineAlert tone="danger">{query.error.message}</InlineAlert>
      ) : !query.data ? (
        <Skeleton height={120} />
      ) : (
        <div className={styles.tableWrap} tabIndex={0} role="region" aria-label="역할 부여 표">
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">사용자</th>
                <th scope="col">역할</th>
                <th scope="col">부여</th>
                <th scope="col">
                  <span className="visually-hidden">회수</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {query.data.map((grant) => (
                <tr key={`${grant.userId}-${grant.role}`}>
                  <td>
                    <Id value={grant.userId} mine={me.userId} />
                  </td>
                  <td>{ROLE_LABEL[grant.role] ?? grant.role}</td>
                  <td>
                    <Id value={grant.grantedBy} mine={me.userId} /> · <When iso={grant.grantedAt} />
                  </td>
                  <td className={styles.actionsCell}>
                    <Button size="dense" variant="tertiary" onClick={() => setTarget(grant)}>
                      회수
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {target && (
        <DecisionDialog
          open
          onClose={() => setTarget(null)}
          title="역할 회수"
          description={`${ROLE_LABEL[target.role]} 역할을 거둡니다. 마지막 보안 관리자는 회수할 수 없습니다.`}
          confirm="회수"
          danger
          pending={revoke.isPending}
          error={revoke.error}
          onConfirm={() => revoke.mutate(target, { onSuccess: () => setTarget(null) })}
        />
      )}
    </section>
  )
}
