import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'wouter'
import { Badge, Button, InlineAlert, Select, Skeleton, Tabs } from '../../design'
import { adminApi } from './adminApi'
import type { ArenaDonation, Me, ReportedPost } from './adminApi'
import { Clear, DecisionDialog, Id, When, useDecision } from './common'
import styles from './AdminPage.module.css'

/** 검수자가 세울 수 있는 결함군 — 손으로 적는 입력으로 깨뜨릴 수 있는 것만 (DefectKind.reachableByHandWrittenCase). */
const DEFECT_KINDS = [
  ['OFF_BY_ONE', '경계 어긋남'],
  ['MISSING_EDGE_CASE', '빠진 경계 입력'],
  ['WRONG_BRANCH', '잘못된 분기'],
  ['WRONG_ALGORITHM', '잘못된 접근'],
] as const

const KIND_LABEL = { QUESTION: '질문', ANSWER: '답', SOLUTION: '풀이' } as const

/**
 * 신고·기부 검수 (§8.3 익명화된 오답, §8.5 신고·제재). REVIEWER.
 *
 * 게시판 신고와 아레나(기부된 오답과 그 신고)를 탭 둘로. 검수자에게는 잠금이 없다 — 글과 소스가
 * 통째로 보인다. 내리거나 기각하는 것과 사람을 제재하는 것은 다른 결정이고 다른 역할이다 — 여기서는
 * 제재 화면으로 근거를 들고 가는 링크만 준다.
 */
export function ReportsQueue({ me }: { me: Me }) {
  const [tab, setTab] = useState<'discussion' | 'arena'>('discussion')
  return (
    <Tabs
      label="검수 종류"
      value={tab}
      onChange={setTab}
      items={[
        { key: 'discussion', label: '게시판 신고' },
        { key: 'arena', label: '아레나 기부·신고' },
      ]}
    >
      <div className={styles.tabBody}>{tab === 'discussion' ? <DiscussionReports me={me} /> : <Arena />}</div>
    </Tabs>
  )
}

const REFRESH = [['admin', 'discussions'], ['admin', 'arena'], ['admin', 'count', 'reports']]

function DiscussionReports({ me }: { me: Me }) {
  const query = useQuery({ queryKey: ['admin', 'discussions'], queryFn: adminApi.discussionQueue })
  if (query.isError) return <InlineAlert tone="danger">{query.error.message}</InlineAlert>
  if (!query.data) return <Skeleton height={160} />
  if (query.data.length === 0) return <Clear>열린 게시판 신고가 없습니다.</Clear>
  return (
    <ul className={styles.cards}>
      {query.data.map((item) => (
        <li key={item.report.id}>
          <ReportedPostCard item={item} me={me} />
        </li>
      ))}
    </ul>
  )
}

function ReportedPostCard({ item, me }: { item: ReportedPost; me: Me }) {
  const [dialog, setDialog] = useState<'hide' | 'dismiss' | null>(null)
  const resolve = useDecision(
    ({ hide, resolution }: { hide: boolean; resolution: string }) => adminApi.resolveDiscussion(item.report.id, hide, resolution),
    '신고를 처리했습니다',
    REFRESH,
  )
  const { report, post } = item
  const security = me.roles.includes('SECURITY_ADMIN')

  return (
    <article className={styles.card} aria-label={`신고 — ${report.reason}`}>
      <header className={styles.cardHead}>
        <Badge tone="warning">신고</Badge>
        <span className={styles.cardTitle}>{report.reason}</span>
        <span className={styles.cardMeta}>
          신고자 <Id value={report.reporterId} /> · <When iso={report.createdAt} />
        </span>
      </header>
      {post ? (
        <div className={styles.quoted}>
          <p className={styles.cardMeta}>
            <Badge>{KIND_LABEL[post.kind]}</Badge> <code>{post.problemId}</code> · 글쓴이 <Id value={post.authorId} />
          </p>
          {post.title && <p className={styles.quotedTitle}>{post.title}</p>}
          <pre className={styles.quotedBody} tabIndex={0}>
            {post.body}
          </pre>
        </div>
      ) : (
        <p className={styles.muted}>신고된 글이 없습니다 (지워졌습니다).</p>
      )}
      <div className={styles.cardActions}>
        <Button size="dense" variant="danger" onClick={() => setDialog('hide')} disabled={!post}>
          글 내리기
        </Button>
        <Button size="dense" onClick={() => setDialog('dismiss')}>
          기각
        </Button>
        {security && post?.authorId && (
          <Link href={`/admin/sanctions?user=${post.authorId}&evidence=report:${report.id}`} className="linklike">
            제재 검토로 →
          </Link>
        )}
      </div>
      {dialog && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title={dialog === 'hide' ? '글 내리기' : '신고 기각'}
          description={dialog === 'hide' ? '글이 목록과 본문에서 사라지고, 이 글의 열린 신고가 함께 닫힙니다.' : '글은 그대로 두고 신고만 닫습니다.'}
          confirm={dialog === 'hide' ? '내리기' : '기각'}
          danger={dialog === 'hide'}
          reasonLabel="처리 내용"
          pending={resolve.isPending}
          error={resolve.error}
          onConfirm={(resolution) => resolve.mutate({ hide: dialog === 'hide', resolution }, { onSuccess: () => setDialog(null) })}
        />
      )}
    </article>
  )
}

function Arena() {
  const query = useQuery({ queryKey: ['admin', 'arena'], queryFn: adminApi.arenaQueue })
  if (query.isError) return <InlineAlert tone="danger">{query.error.message}</InlineAlert>
  if (!query.data) return <Skeleton height={160} />
  const { pending, reports } = query.data
  return (
    <>
      <h2 className={styles.subTitle}>검수를 기다리는 기부 {pending.length}</h2>
      {pending.length === 0 ? (
        <Clear>검수를 기다리는 기부가 없습니다.</Clear>
      ) : (
        <ul className={styles.cards}>
          {pending.map((donation) => (
            <li key={donation.id}>
              <DonationCard donation={donation} />
            </li>
          ))}
        </ul>
      )}
      <h2 className={styles.subTitle}>과녁 신고 {reports.length}</h2>
      {reports.length === 0 ? (
        <Clear>열린 과녁 신고가 없습니다.</Clear>
      ) : (
        <ul className={styles.cards}>
          {reports.map((item) => (
            <li key={item.report.id}>
              <ArenaReportCard report={item.report} donation={item.donation} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}

function Source({ donation }: { donation: ArenaDonation | null }) {
  if (!donation) return <p className={styles.muted}>기부가 없습니다.</p>
  return (
    <div className={styles.quoted}>
      <p className={styles.cardMeta}>
        <code>{donation.problemId}</code> · 기부자 <Id value={donation.donorUserId} /> · 제출 <Id value={donation.submissionId} />
      </p>
      {donation.note && <p>{donation.note}</p>}
      {donation.source === null ? (
        <p className={styles.muted}>기부자가 계정을 지워 소스가 없습니다.</p>
      ) : (
        <pre className={styles.code} tabIndex={0} aria-label="기부된 오답 소스">
          {donation.source}
        </pre>
      )}
    </div>
  )
}

function DonationCard({ donation }: { donation: ArenaDonation }) {
  const [dialog, setDialog] = useState<'approve' | 'reject' | null>(null)
  const [kind, setKind] = useState<string>(DEFECT_KINDS[0][0])
  const approve = useDecision((note: string) => adminApi.approveDonation(donation.id, kind, note || null), '과녁으로 세웠습니다', REFRESH)
  const reject = useDecision((reason: string) => adminApi.rejectDonation(donation.id, reason), '기부를 반려했습니다', REFRESH)

  return (
    <article className={styles.card} aria-label={`기부 — ${donation.problemId}`}>
      <header className={styles.cardHead}>
        <Badge tone="trace">기부</Badge>
        <span className={styles.cardMeta}>
          <When iso={donation.createdAt} />
        </span>
      </header>
      <Source donation={donation} />
      <div className={styles.cardActions}>
        <Button size="dense" variant="primary" onClick={() => setDialog('approve')} disabled={donation.source === null}>
          과녁으로 세우기
        </Button>
        <Button size="dense" onClick={() => setDialog('reject')}>
          반려
        </Button>
      </div>
      {dialog === 'approve' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title="과녁으로 세우기"
          description="결함군은 검수자가 정합니다 — 기부자는 자기 오답이 무슨 종류인지 모르는 것이 보통입니다."
          confirm="세우기"
          reasonLabel="메모"
          reasonRequired={false}
          pending={approve.isPending}
          error={approve.error}
          onConfirm={(note) => approve.mutate(note, { onSuccess: () => setDialog(null) })}
        >
          <Select label="결함군" value={kind} onChange={(event) => setKind(event.target.value)}>
            {DEFECT_KINDS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </DecisionDialog>
      )}
      {dialog === 'reject' && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title="기부 반려"
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

function ArenaReportCard({ report, donation }: { report: { id: string; reason: string; reporterId: string; createdAt: string }; donation: ArenaDonation | null }) {
  const [dialog, setDialog] = useState<'retire' | 'dismiss' | null>(null)
  const resolve = useDecision(
    ({ retire, resolution }: { retire: boolean; resolution: string }) => adminApi.resolveArenaReport(report.id, retire, resolution),
    '신고를 처리했습니다',
    REFRESH,
  )
  return (
    <article className={styles.card} aria-label={`과녁 신고 — ${report.reason}`}>
      <header className={styles.cardHead}>
        <Badge tone="warning">신고</Badge>
        <span className={styles.cardTitle}>{report.reason}</span>
        <span className={styles.cardMeta}>
          신고자 <Id value={report.reporterId} /> · <When iso={report.createdAt} />
        </span>
      </header>
      <Source donation={donation} />
      <div className={styles.cardActions}>
        <Button size="dense" variant="danger" onClick={() => setDialog('retire')}>
          과녁 내리기
        </Button>
        <Button size="dense" onClick={() => setDialog('dismiss')}>
          기각
        </Button>
      </div>
      {dialog && (
        <DecisionDialog
          open
          onClose={() => setDialog(null)}
          title={dialog === 'retire' ? '과녁 내리기' : '신고 기각'}
          confirm={dialog === 'retire' ? '내리기' : '기각'}
          danger={dialog === 'retire'}
          reasonLabel="처리 내용"
          pending={resolve.isPending}
          error={resolve.error}
          onConfirm={(resolution) => resolve.mutate({ retire: dialog === 'retire', resolution }, { onSuccess: () => setDialog(null) })}
        />
      )}
    </article>
  )
}
