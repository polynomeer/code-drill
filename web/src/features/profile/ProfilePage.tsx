import { useQuery } from '@tanstack/react-query'
import { Flame, Lock, Settings, Trophy } from 'lucide-react'
import { useState } from 'react'
import { Link, Redirect, useLocation } from 'wouter'
import { getProfile, getProfileSettings } from '../../api/client'
import { useSession } from '../../api/session'
import { Badge, Button, Dialog, DifficultyBadge, EmptyState, Skeleton } from '../../design'
import { DIFFICULTIES } from '../../shared/types'
import type { PublicProfile } from '../../shared/types'
import { fullTime, relativeTime } from '../../shared/time'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { signed } from '../contest/contestView'
import { ActivityHeatmap } from './ActivityHeatmap'
import { ProfileSettingsForm } from './ProfileSettingsForm'
import { RatingChart } from './RatingChart'
import { TIER_LABEL } from './profileView'
import styles from './ProfilePage.module.css'

/**
 * 프로필 `/u/:handle` (docs/ui-overhaul.md §6.7).
 *
 * 공개로 켠 사람의 것만 남에게 보인다. 비공개거나 없는 핸들은 같은 빈 화면이다 — 둘을 가르면 그
 * 핸들을 쓰는 사람이 있다는 사실이 샌다. 본인은 비공개여도 자기 프로필을 미리 본다.
 *
 * **역량 수준은 없다.** 본인에게는 "내 역량 지도" 링크만 — 공개 프로필에 내걸면 No false precision 과
 * 어긋나는 비교가 시작된다.
 */
export function ProfilePage({ handle }: { handle: string }) {
  if (handle === 'me') return <MyProfile />
  return <Profile handle={handle} />
}

/** `/u/me` — 핸들이 있으면 그 주소로, 없으면 여기서 정한다. */
function MyProfile() {
  const session = useSession()
  const [, navigate] = useLocation()
  const settings = useQuery({ queryKey: ['me', 'profile-settings'], queryFn: getProfileSettings, enabled: session !== null })

  if (!session) return <Redirect to={`/login?next=${encodeURIComponent('/u/me')}`} replace />
  if (settings.data?.handle) return <Redirect to={`/u/${settings.data.handle}`} replace />
  if (settings.isPending) {
    return (
      <div className={styles.page} role="status" aria-busy="true">
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton height={160} />
      </div>
    )
  }
  return (
    <div className={styles.page}>
      <h1 className={styles.name}>내 프로필</h1>
      <p className={styles.muted}>프로필 주소로 쓸 핸들을 정하면 내 프로필이 생깁니다. 공개는 따로 켭니다 — 켜기 전에는 나만 봅니다.</p>
      <ProfileSettingsForm onSaved={(saved) => saved && navigate(`/u/${saved}`, { replace: true })} />
    </div>
  )
}

function Profile({ handle }: { handle: string }) {
  const query = useQuery({ queryKey: ['profile', handle.toLowerCase()], queryFn: () => getProfile(handle) })
  const [settingsOpen, setSettingsOpen] = useState(false)

  if (query.isError) {
    return (
      <div className={styles.page}>
        <EmptyState title="프로필을 불러오지 못했습니다">잠시 뒤 다시 시도하세요.</EmptyState>
      </div>
    )
  }
  if (query.isPending) {
    return (
      <div className={styles.page} role="status" aria-busy="true">
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton height={80} />
        <Skeleton height={140} />
        <Skeleton height={160} />
      </div>
    )
  }
  const profile = query.data
  if (!profile) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="프로필이 없거나 공개되지 않았습니다"
          action={
            <Link href="/problems" className="linklike">
              문제 둘러보기
            </Link>
          }
        >
          주소의 핸들을 확인하세요. 프로필은 주인이 공개로 켠 것만 보입니다.
        </EmptyState>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <span className={styles.avatar} aria-hidden="true">
          {profile.displayName.slice(0, 1)}
        </span>
        <div className={styles.identity}>
          <h1 className={styles.name}>{profile.displayName}</h1>
          <p className={styles.muted}>
            @{profile.handle} · {new Date(profile.joinedAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' })} 가입
          </p>
        </div>
        {profile.mine && (
          <div className={styles.ownerActions}>
            {profile.public ? <Badge tone="success">공개</Badge> : <Badge icon={<Lock size={12} />}>비공개 — 나만 봄</Badge>}
            <Button size="dense" icon={<Settings size={14} />} onClick={() => setSettingsOpen(true)}>
              공개 설정
            </Button>
          </div>
        )}
      </header>

      {profile.mine && (
        <p className={styles.ownerNote}>
          역량 수준은 프로필에 싣지 않습니다 — 나만 봅니다.{' '}
          <Link href="/competencies" className="linklike">
            내 역량 지도
          </Link>
        </p>
      )}

      <Stats profile={profile} />

      <section aria-labelledby="activity-heading" className={styles.section}>
        <h2 id="activity-heading" className={styles.sectionTitle}>
          최근 1년 활동
        </h2>
        <ActivityHeatmap days={profile.activity} />
      </section>

      {profile.rating && (
        <section aria-labelledby="rating-heading" className={styles.section}>
          <h2 id="rating-heading" className={styles.sectionTitle}>
            레이팅
          </h2>
          <RatingChart history={profile.rating.history} />
          <RatingHistory history={profile.rating.history} />
        </section>
      )}

      <Solutions profile={profile} />

      {profile.mine && (
        <Dialog open={settingsOpen} onClose={() => setSettingsOpen(false)} title="공개 프로필 설정">
          <ProfileSettingsForm onSaved={() => setSettingsOpen(false)} />
        </Dialog>
      )}
    </div>
  )
}

function Stats({ profile }: { profile: PublicProfile }) {
  const most = Math.max(1, ...Object.values(profile.solved.byDifficulty).map((n) => n ?? 0))
  return (
    <ul className={styles.stats} aria-label="요약">
      <li className={styles.stat}>
        <span className={styles.statLabel}>푼 문제</span>
        <span className={styles.statValue}>{profile.solved.total}</span>
        {profile.solved.total > 0 && (
          <ul className={styles.difficulties} aria-label="난이도별">
            {DIFFICULTIES.filter((level) => profile.solved.byDifficulty[level]).map((level) => (
              <li key={level}>
                <DifficultyBadge level={level} />
                <span className={styles.bar} aria-hidden="true">
                  <span style={{ width: `${((profile.solved.byDifficulty[level] ?? 0) / most) * 100}%` }} />
                </span>
                <span className={styles.statCount}>{profile.solved.byDifficulty[level]}</span>
              </li>
            ))}
          </ul>
        )}
      </li>
      <li className={styles.stat}>
        <span className={styles.statLabel}>
          <Flame size={14} aria-hidden="true" /> 연속 일수
        </span>
        <span className={styles.statValue}>{profile.streak.current}일</span>
        <span className={styles.muted}>1년 안 가장 길게 {profile.streak.longest}일</span>
      </li>
      <li className={styles.stat}>
        <span className={styles.statLabel}>
          <Trophy size={14} aria-hidden="true" /> 레이팅
        </span>
        {profile.rating ? (
          <>
            <span className={styles.statValue}>{profile.rating.rating}</span>
            <span className={styles.muted}>
              레이팅 대회 {profile.rating.contests}회{profile.rating.contests < 5 && ' · 잠정'}
            </span>
          </>
        ) : (
          // 출발점(1500)을 실력처럼 내걸지 않는다
          <span className={styles.muted}>레이팅 대회 기록이 없습니다</span>
        )}
      </li>
      <li className={styles.stat}>
        <span className={styles.statLabel}>기여</span>
        <span className={styles.statValueSmall}>{TIER_LABEL[profile.contributorTier]}</span>
        <span className={styles.muted}>공개한 풀이 {profile.solutions.length}개</span>
      </li>
    </ul>
  )
}

function RatingHistory({ history }: { history: NonNullable<PublicProfile['rating']>['history'] }) {
  return (
    <div className={styles.tableWrap} tabIndex={0} role="region" aria-label="대회 이력 표">
      <table className={styles.table}>
        <caption className="visually-hidden">대회 이력 — 최근 것부터</caption>
        <thead>
          <tr>
            <th scope="col">대회</th>
            <th scope="col">순위</th>
            <th scope="col">변화</th>
            <th scope="col">레이팅</th>
            <th scope="col">날짜</th>
          </tr>
        </thead>
        <tbody>
          {history.map((change) => (
            <tr key={change.contestId}>
              <td>
                <Link href={`/contests/${change.contestId}`}>{change.title}</Link>
              </td>
              <td className={styles.num}>{change.rank}위</td>
              <td className={`${styles.num} ${change.after >= change.before ? styles.up : styles.down}`}>{signed(change.after - change.before)}</td>
              <td className={styles.num}>{change.after}</td>
              <td className={styles.num}>
                <time dateTime={change.at} title={fullTime(change.at)}>
                  {new Date(change.at).toLocaleDateString('ko-KR')}
                </time>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Solutions({ profile }: { profile: PublicProfile }) {
  const index = useProblemIndex()
  return (
    <section aria-labelledby="solutions-heading" className={styles.section}>
      <h2 id="solutions-heading" className={styles.sectionTitle}>
        공개한 풀이
      </h2>
      {profile.solutions.length === 0 ? (
        <p className={styles.muted}>아직 공개한 풀이가 없습니다.</p>
      ) : (
        <ul className={styles.solutions}>
          {profile.solutions.map((solution) => (
            <li key={solution.postId}>
              {/* 본문은 문제 화면에서 — 맞히기 전에는 잠긴다는 규칙이 거기 있다 */}
              <Link href={`/problems/${solution.problemId}`} className={styles.solutionTitle}>
                {solution.title ?? '제목 없는 풀이'}
              </Link>
              <span className={styles.muted}>
                {problemLabel(index, solution.problemId)} · 도움됐다 {solution.helpful} ·{' '}
                <time dateTime={solution.at} title={fullTime(solution.at)}>
                  {relativeTime(solution.at)}
                </time>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
