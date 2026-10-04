import { useQuery } from '@tanstack/react-query'
import { ClipboardCheck, FileClock, Flag, Fingerprint, Gavel, RefreshCcw, ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'wouter'
import { Badge, EmptyState, Skeleton } from '../../design'
import { AdminError, ROLE_LABEL, adminApi } from './adminApi'
import type { AdminRole, Me } from './adminApi'
import { AuditView } from './AuditView'
import { IntegrityQueue } from './IntegrityQueue'
import { OperatorsView } from './OperatorsView'
import { RejudgeQueue } from './RejudgeQueue'
import { ReportsQueue } from './ReportsQueue'
import { SanctionsQueue } from './SanctionsQueue'
import { VersionsQueue } from './VersionsQueue'
import styles from './AdminPage.module.css'

type QueueKey = 'versions' | 'rejudges' | 'reports' | 'integrity' | 'sanctions' | 'operators' | 'audit'

/**
 * 큐마다 볼 수 있는 역할. 서버의 @RequiresRole 과 같은 표다 — 여기서 숨기는 것은 편의이고, 막는 것은
 * 서버다. 역할이 없는 큐를 보여 주면 단추마다 403 을 받는 화면이 된다.
 */
const QUEUES: { key: QueueKey; label: string; icon: ReactNode; roles: AdminRole[]; count?: (me: Me) => Promise<number> }[] = [
  { key: 'versions', label: '문제 버전 검수', icon: <ClipboardCheck size={16} />, roles: ['CONTENT_EDITOR', 'PUBLISHER'], count: () => adminApi.pendingVersions().then((v) => v.length) },
  {
    key: 'rejudges',
    label: '재채점',
    icon: <RefreshCcw size={16} />,
    roles: ['JUDGE_OPERATOR', 'REVIEWER'],
    count: () => adminApi.rejudges().then((jobs) => jobs.filter((job) => job.status === 'REQUESTED' || job.status === 'APPROVED').length),
  },
  {
    key: 'reports',
    label: '신고·기부 검수',
    icon: <Flag size={16} />,
    roles: ['REVIEWER'],
    count: async () => {
      const [posts, arena] = await Promise.all([adminApi.discussionQueue(), adminApi.arenaQueue()])
      return posts.length + arena.pending.length + arena.reports.length
    },
  },
  { key: 'integrity', label: '유사도 신호', icon: <Fingerprint size={16} />, roles: ['REVIEWER'], count: () => adminApi.integrityQueue().then((q) => q.length) },
  { key: 'sanctions', label: '제재·이의', icon: <Gavel size={16} />, roles: ['SECURITY_ADMIN'], count: () => adminApi.appeals().then((a) => a.length) },
  {
    key: 'operators',
    label: '운영자 역할',
    icon: <ShieldCheck size={16} />,
    roles: ['SECURITY_ADMIN'],
    count: () => adminApi.roleRequests().then((r) => r.length),
  },
  { key: 'audit', label: '감사 로그', icon: <FileClock size={16} />, roles: ['SECURITY_ADMIN'] },
]

/**
 * 운영 콘솔 `/admin/:queue` (docs/ui-overhaul.md §6.8).
 *
 * 검수·재채점 승인·제재·유사도 신호·기부 검수가 API 로만 있었다 — 운영자는 curl 로 일했다. 큐마다 화면
 * 하나, 결정마다 사유. 2인 원칙(등록자 ≠ 공개자, 요청자 ≠ 승인자, 발부자 ≠ 이의 판단자)은 서버가 지키고,
 * 화면은 거절 사유를 서버 문장 그대로 보인다.
 *
 * 일반 사용자 번들에 섞이지 않는다 — 라우트 청크로만 온다. 일반 화면에는 이리 오는 링크도 없다:
 * 역할을 묻는 요청 자체가 역할 없는 사람에게는 감사 로그의 거부 기록이 되기 때문이다.
 */
export function AdminPage({ queue }: { queue: string | undefined }) {
  const me = useQuery({ queryKey: ['admin', 'me'], queryFn: adminApi.me, retry: false })

  if (me.isPending) {
    return (
      <div className={styles.page} role="status" aria-busy="true">
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton height={400} />
      </div>
    )
  }

  if (me.isError) {
    const forbidden = me.error instanceof AdminError && me.error.status === 403
    return (
      <div className={styles.page}>
        <EmptyState
          title={forbidden ? '운영 역할이 없습니다' : '운영 콘솔을 열지 못했습니다'}
          action={
            <Link href="/" className="linklike">
              홈으로
            </Link>
          }
        >
          {forbidden ? '이 계정에는 운영 역할이 없습니다. 역할은 보안 관리자가 요청하고 다른 보안 관리자가 승인합니다.' : me.error.message}
        </EmptyState>
      </div>
    )
  }

  const visible = QUEUES.filter((item) => item.roles.some((role) => me.data.roles.includes(role)))
  const current = visible.find((item) => item.key === queue)

  return (
    <div className={styles.console}>
      <nav className={styles.sidebar} aria-label="운영 큐">
        <p className={styles.sidebarTitle}>운영 콘솔</p>
        <ul className={styles.queueList}>
          {visible.map((item) => (
            <li key={item.key}>
              <Link
                href={`/admin/${item.key}`}
                className={item.key === current?.key ? `${styles.queueLink} ${styles.queueActive}` : styles.queueLink}
                aria-current={item.key === current?.key ? 'page' : undefined}
              >
                <span aria-hidden="true">{item.icon}</span>
                <span className={styles.queueLabel}>{item.label}</span>
                {item.count && <QueueCount queue={item.key} count={() => item.count!(me.data)} />}
              </Link>
            </li>
          ))}
        </ul>
        <div className={styles.roles}>
          <p className={styles.sidebarTitle}>내 역할</p>
          <div className={styles.roleBadges}>
            {me.data.roles.map((role) => (
              <Badge key={role}>{ROLE_LABEL[role]}</Badge>
            ))}
          </div>
        </div>
      </nav>

      <section className={styles.main} aria-labelledby="queue-title">
        {current ? (
          <>
            <h1 id="queue-title" className={styles.title}>
              {current.label}
            </h1>
            <Queue queue={current.key} me={me.data} />
          </>
        ) : (
          <Overview visible={visible} me={me.data} />
        )}
      </section>
    </div>
  )
}

function Queue({ queue, me }: { queue: QueueKey; me: Me }) {
  switch (queue) {
    case 'versions':
      return <VersionsQueue me={me} />
    case 'rejudges':
      return <RejudgeQueue me={me} />
    case 'reports':
      return <ReportsQueue me={me} />
    case 'integrity':
      return <IntegrityQueue me={me} />
    case 'sanctions':
      return <SanctionsQueue me={me} />
    case 'operators':
      return <OperatorsView me={me} />
    case 'audit':
      return <AuditView />
  }
}

/** 기다리는 일의 수. 0 이면 숫자 대신 아무것도 — 할 일이 없는 큐가 주의를 끌지 않게. */
function QueueCount({ queue, count }: { queue: QueueKey; count: () => Promise<number> }) {
  const query = useQuery({ queryKey: ['admin', 'count', queue], queryFn: count, refetchInterval: 60_000 })
  if (!query.data) return null
  return (
    <span className={styles.count}>
      {query.data}
      <span className="visually-hidden">건 대기</span>
    </span>
  )
}

function Overview({ visible, me }: { visible: typeof QUEUES; me: Me }) {
  return (
    <>
      <h1 id="queue-title" className={styles.title}>
        운영 콘솔
      </h1>
      <p className={styles.muted}>역할이 닿는 큐만 보입니다. 결정마다 사유를 받고, 사유와 결정은 감사 로그에 남습니다.</p>
      <ul className={styles.overview}>
        {visible.map((item) => (
          <li key={item.key}>
            <Link href={`/admin/${item.key}`} className={styles.overviewCard}>
              <span className={styles.overviewHead}>
                <span aria-hidden="true">{item.icon}</span>
                {item.label}
              </span>
              {item.count ? (
                <OverviewCount queue={item.key} count={() => item.count!(me)} />
              ) : (
                <span className={styles.muted}>열람</span>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}

function OverviewCount({ queue, count }: { queue: QueueKey; count: () => Promise<number> }) {
  const query = useQuery({ queryKey: ['admin', 'count', queue], queryFn: count })
  if (query.isError) return <span className={styles.muted}>불러오지 못함</span>
  if (query.data === undefined) return <Skeleton width={60} height={20} />
  return <span className={query.data > 0 ? styles.overviewCount : styles.muted}>{query.data > 0 ? `${query.data}건 대기` : '대기 없음'}</span>
}
