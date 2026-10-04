import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Clock, Copy, LocateFixed } from 'lucide-react'
import { useRef } from 'react'
import { Link, useLocation } from 'wouter'
import { getContest, joinContest, listProjects, startVirtual } from '../../api/client'
import { Badge, Button, EmptyState, InlineAlert, Skeleton, useToast } from '../../design'
import type { ContestView, Standing } from '../../shared/types'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { KIND_LABEL, STATUS_LABEL, clock, countdownOf, remaining, signed } from './contestView'
import { useNow } from './useNow'
import styles from './ContestPage.module.css'

/**
 * 대회 화면 `/contests/:id` (docs/ui-overhaul.md §6.6).
 *
 * 헤더가 고정이다 — 남은 시간은 어디까지 스크롤했든 보여야 한다. 왼쪽은 문제와 내 점수, 오른쪽은
 * 순위표(머리 고정, 내 줄 강조). 진행 중이면 순위표를 30초마다 다시 읽는다 — 판정이 끝날 때마다
 * 움직이는 곳이지만, 매초 묻는 것은 서버를 두드리는 시계다.
 *
 * 참가는 곧 이름 공개 동의다. 그 말을 버튼 옆에서 미리 한다.
 */
export function ContestPage({ id }: { id: string }) {
  const query = useQuery({
    queryKey: ['contest', id],
    queryFn: () => getContest(id),
    refetchInterval: (current) => (current.state.data?.contest.status === 'RUNNING' ? 30_000 : false),
  })

  if (query.isError) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="대회를 찾지 못했습니다"
          action={
            <Link href="/contests" className="linklike">
              대회 목록으로
            </Link>
          }
        >
          공개되지 않았거나, 참가하지 않은 대결입니다.
        </EmptyState>
      </div>
    )
  }

  if (!query.data) {
    return (
      <div className={styles.page} role="status" aria-busy="true">
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton height={64} />
        <Skeleton height={320} />
      </div>
    )
  }

  return <Contest view={query.data} />
}

function Contest({ view }: { view: ContestView }) {
  const { contest } = view
  const client = useQueryClient()
  const toast = useToast()
  const [, navigate] = useLocation()
  const index = useProblemIndex()
  const projects = useQuery({ queryKey: ['projects'], queryFn: listProjects, staleTime: 5 * 60_000 })
  const projectIds = new Set((projects.data ?? []).map((project) => project.id))
  const countdown = countdownOf(contest)
  const now = useNow(countdown !== null)
  const mine = view.standings.find((row) => row.mine)
  const table = useRef<HTMLDivElement>(null)

  const join = useMutation({
    mutationFn: () => joinContest(contest.id),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['contest', contest.id] })
      toast.show('참가했습니다. 이름이 이 대회의 순위표에 오릅니다.', 'success')
    },
    onError: (error) => toast.show(error instanceof Error ? error.message : '참가하지 못했습니다', 'danger'),
  })
  const virtual = useMutation({
    mutationFn: () => startVirtual(contest.id),
    onSuccess: (result) => navigate(`/contests/${result.contest.id}`),
    onError: (error) => toast.show(error instanceof Error ? error.message : '가상 참가를 열지 못했습니다', 'danger'),
  })

  // 프로젝트형 문제는 풀이 화면이 아니라 홈의 프로젝트 작업 공간에서 연다 (11단계)
  const problemHref = (problemId: string) => (projectIds.has(problemId) ? `/?project=${problemId}` : `/problems/${problemId}/solve`)
  const hack = contest.kind === 'HACK'

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerMain}>
          <Link href="/contests" className={styles.back} aria-label="대회 목록으로">
            <ArrowLeft size={18} aria-hidden="true" />
          </Link>
          <div className={styles.titleBlock}>
            <h1 className={styles.title}>{contest.title}</h1>
            <p className={styles.meta}>
              <Badge>{KIND_LABEL[contest.kind]}</Badge>
              {contest.rated && <Badge tone="brand">레이팅</Badge>}
              <Badge tone={contest.status === 'RUNNING' ? 'success' : 'neutral'}>{STATUS_LABEL[contest.status]}</Badge>
              <span>참가 {contest.entrants}</span>
            </p>
          </div>
        </div>
        {countdown && (
          <p className={styles.countdown} role="timer" aria-label={`${countdown.label} ${remaining(countdown.target, now)}`}>
            <Clock size={16} aria-hidden="true" />
            <span className={styles.countdownLabel}>{countdown.label}</span>
            <span className={styles.countdownValue} aria-hidden="true">
              {remaining(countdown.target, now)}
            </span>
          </p>
        )}
      </header>

      {hack && (
        <InlineAlert tone="info">
          문제를 맞힌 뒤 아레나에서 깨뜨린 서로 다른 오답의 수가 점수입니다. 정답 자체는 점수가 아닙니다.
        </InlineAlert>
      )}

      {view.joinCode && (
        <InlineAlert
          tone="info"
          title={
            <>
              상대에게 알릴 코드 <code className={styles.code}>{view.joinCode}</code>
            </>
          }
          action={
            <Button
              size="dense"
              icon={<Copy size={14} />}
              onClick={() => {
                void navigator.clipboard?.writeText(view.joinCode ?? '')
                toast.show('코드를 복사했습니다', 'success')
              }}
            >
              복사
            </Button>
          }
        >
          상대가 이 코드로 붙는 순간 시작합니다.
        </InlineAlert>
      )}

      {!contest.joined && contest.kind === 'CONTEST' && contest.status !== 'FINISHED' && (
        <div className={styles.join}>
          <Button variant="primary" loading={join.isPending} onClick={() => join.mutate()}>
            참가하기
          </Button>
          <span className={styles.muted}>참가하면 표시 이름이 이 대회의 순위표에 오릅니다.</span>
        </div>
      )}

      {/* 끝난 대회는 같은 시간 조건으로 혼자 다시 돈다 (§8.4 가상 참가) */}
      {contest.kind === 'CONTEST' && contest.status === 'FINISHED' && (
        <div className={styles.join}>
          {view.virtual ? (
            <Link href={`/contests/${view.virtual}`} className="linklike">
              돌고 있는 가상 참가 열기
            </Link>
          ) : (
            <>
              <Button loading={virtual.isPending} onClick={() => virtual.mutate()}>
                가상 참가
              </Button>
              <span className={styles.muted}>같은 문제, 같은 길이로 지금부터. 그때 참가했다면 몇 등이었을지 봅니다.</span>
            </>
          )}
        </div>
      )}

      <div className={styles.body}>
        <section aria-labelledby="problems-heading" className={styles.problems}>
          <h2 id="problems-heading" className={styles.sectionTitle}>
            문제
          </h2>
          <ol className={styles.problemList}>
            {view.problems.map((problemId, position) => {
              const score = mine?.perProblem[problemId]
              return (
                <li key={problemId}>
                  <span className={styles.problemLetter} aria-hidden="true">
                    {String.fromCharCode(65 + position)}
                  </span>
                  <Link href={problemHref(problemId)} className={styles.problemLink}>
                    {problemLabel(index, problemId)}
                  </Link>
                  <span className={styles.problemScore}>
                    {score === undefined ? (
                      <span className={styles.muted}>아직 없음</span>
                    ) : hack ? (
                      `${score}개`
                    ) : (
                      <Badge tone={score >= 100 ? 'success' : score > 0 ? 'warning' : 'neutral'}>{score}점</Badge>
                    )}
                  </span>
                </li>
              )
            })}
          </ol>
        </section>

        <section aria-labelledby="standings-heading" className={styles.standings}>
          <div className={styles.standingsHead}>
            <h2 id="standings-heading" className={styles.sectionTitle}>
              순위표
            </h2>
            {mine && (
              <Button
                size="dense"
                variant="tertiary"
                icon={<LocateFixed size={14} />}
                onClick={() => {
                  const row = table.current?.querySelector<HTMLElement>('[data-mine="true"]')
                  row?.scrollIntoView({ block: 'center' })
                  row?.focus()
                }}
              >
                내 순위 {mine.rank}위
              </Button>
            )}
          </div>
          {view.standings.length === 0 ? (
            <p className={styles.muted}>아직 참가자가 없습니다.</p>
          ) : (
            <div className={styles.tableWrap} ref={table} tabIndex={0} role="region" aria-label="순위표 표">
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th scope="col">#</th>
                    <th scope="col">이름</th>
                    <th scope="col">{hack ? '깨뜨린 오답' : '총점'}</th>
                    <th scope="col">{hack ? '문제' : '푼 문제'}</th>
                    {!hack && <th scope="col">걸린 시간</th>}
                    {contest.ratedAt && <th scope="col">레이팅</th>}
                  </tr>
                </thead>
                <tbody>
                  {view.standings.map((row) => (
                    <StandingRow key={`${row.rank}-${row.displayName}`} row={row} hack={hack} rated={contest.ratedAt !== null} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function StandingRow({ row, hack, rated }: { row: Standing; hack: boolean; rated: boolean }) {
  return (
    <tr data-mine={row.mine || undefined} tabIndex={row.mine ? -1 : undefined} className={row.mine ? styles.mineRow : undefined}>
      <td className={styles.num}>{row.rank}</td>
      <td>
        {row.displayName}
        {row.mine && <Badge tone="brand">나</Badge>}
        {row.virtual && <Badge>가상</Badge>}
      </td>
      <td className={styles.num}>{row.total}</td>
      <td className={styles.num}>{row.solved}</td>
      {!hack && <td className={styles.num}>{row.elapsedSeconds === null ? '—' : clock(row.elapsedSeconds)}</td>}
      {rated && (
        <td className={`${styles.num} ${row.ratingChange !== null && row.ratingChange < 0 ? styles.down : styles.up}`}>
          {row.ratingChange === null ? '' : signed(row.ratingChange)}
        </td>
      )}
    </tr>
  )
}
