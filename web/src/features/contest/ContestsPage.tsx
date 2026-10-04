import { useMutation, useQuery } from '@tanstack/react-query'
import { Swords, Trophy } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation } from 'wouter'
import { joinDuel, listContests, listFinishedContests } from '../../api/client'
import { Badge, Button, InlineAlert, Pagination, Skeleton, TextField } from '../../design'
import type { ContestSummary } from '../../shared/types'
import { date } from '../../shared/format'
import { setParam } from '../../shared/url'
import { KIND_LABEL, STATUS_LABEL, countdownOf, remaining, sectionOf } from './contestView'
import { useNow } from './useNow'
import styles from './ContestsPage.module.css'

/**
 * 대회 로비 `/contests` (docs/ui-overhaul.md §6.6).
 *
 * 진행 중 / 예정 / 끝남 세 구획. 끝난 대회는 쌓이기만 하므로 쪽으로 나눠 서버에서 받는다 — 예전에는
 * 끝난 것 50개가 진행 중인 것과 한 목록에 섞여, 지금 들어갈 수 있는 대회가 묻혔다.
 *
 * 미니 대결은 여기서 코드로 붙는다. 여는 것은 풀이 화면의 "미니 대결" — 문제를 고른 자리에서다.
 */
export function ContestsPage() {
  const [page, setPage] = useState(() => Math.max(1, Number(new URLSearchParams(window.location.search).get('page')) || 1))
  const active = useQuery({ queryKey: ['contests', 'active'], queryFn: () => listContests('active'), refetchInterval: 60_000 })
  const finished = useQuery({ queryKey: ['contests', 'finished', page], queryFn: () => listFinishedContests(page) })

  const running = (active.data ?? []).filter((contest) => sectionOf(contest) === 'running')
  const upcoming = (active.data ?? []).filter((contest) => sectionOf(contest) === 'upcoming')

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <h1 className={styles.title}>대회</h1>
      </header>

      {active.isError && <InlineAlert tone="danger" title="대회 목록을 불러오지 못했습니다" />}

      <Section id="running" title="진행 중" loading={active.isPending} contests={running} empty="지금 진행 중인 대회가 없습니다." />
      <Section id="upcoming" title="예정" loading={active.isPending} contests={upcoming} empty="예정된 대회가 없습니다." />

      <DuelJoin />

      <section aria-labelledby="finished-heading" className={styles.section}>
        <h2 id="finished-heading" className={styles.sectionTitle}>
          끝남
          {finished.data && <span className={styles.count}>{finished.data.total}</span>}
        </h2>
        <p className={styles.muted}>끝난 대회는 같은 문제·같은 시간으로 혼자 다시 돌 수 있습니다 (가상 참가).</p>
        {finished.isError ? (
          <InlineAlert tone="danger">끝난 대회를 불러오지 못했습니다.</InlineAlert>
        ) : !finished.data ? (
          <Skeleton height={120} />
        ) : finished.data.items.length === 0 ? (
          <p className={styles.muted}>아직 끝난 대회가 없습니다.</p>
        ) : (
          <>
            <ContestList contests={finished.data.items} />
            <Pagination
              page={finished.data.page}
              pageCount={finished.data.pageCount}
              label="끝난 대회 쪽 이동"
              onChange={(next) => {
                setPage(next)
                setParam('page', next === 1 ? null : String(next))
              }}
            />
          </>
        )}
      </section>
    </div>
  )
}

function Section({
  id,
  title,
  loading,
  contests,
  empty,
}: {
  id: string
  title: string
  loading: boolean
  contests: ContestSummary[]
  empty: string
}) {
  return (
    <section aria-labelledby={`${id}-heading`} className={styles.section}>
      <h2 id={`${id}-heading`} className={styles.sectionTitle}>
        {title}
        {!loading && <span className={styles.count}>{contests.length}</span>}
      </h2>
      {loading ? <Skeleton height={72} /> : contests.length === 0 ? <p className={styles.muted}>{empty}</p> : <ContestList contests={contests} />}
    </section>
  )
}

function ContestList({ contests }: { contests: ContestSummary[] }) {
  const now = useNow(contests.some((contest) => countdownOf(contest) !== null))
  return (
    <ul className={styles.list}>
      {contests.map((contest) => {
        const countdown = countdownOf(contest)
        return (
          <li key={contest.id}>
            <Link href={`/contests/${contest.id}`} className={styles.row}>
              <span className={styles.rowIcon} aria-hidden="true">
                {contest.kind === 'DUEL' ? <Swords size={18} /> : <Trophy size={18} />}
              </span>
              <span className={styles.rowMain}>
                <span className={styles.rowTitle}>{contest.title}</span>
                <span className={styles.rowMeta}>
                  <Badge>{KIND_LABEL[contest.kind]}</Badge>
                  {contest.rated && <Badge tone="brand">레이팅</Badge>}
                  {contest.joined && <Badge tone="success">참가 중</Badge>}
                  <span>
                    문제 {contest.problemCount} · 참가 {contest.entrants}
                    {contest.minutes ? ` · ${contest.minutes}분` : ''}
                  </span>
                </span>
              </span>
              <span className={styles.rowTime}>
                {countdown ? (
                  <>
                    <span className={styles.timeLabel}>{countdown.label}</span>
                    <span className={styles.timeValue}>{remaining(countdown.target, now)}</span>
                  </>
                ) : contest.status === 'FINISHED' && contest.endsAt ? (
                  <span className={styles.timeLabel}>{date(contest.endsAt)} 끝남</span>
                ) : (
                  <span className={styles.timeLabel}>{STATUS_LABEL[contest.status]}</span>
                )}
              </span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}

/** 받은 코드로 미니 대결에 붙는다. 붙는 순간 시작하므로 바로 그 대결 화면으로 간다. */
function DuelJoin() {
  const [, navigate] = useLocation()
  const [code, setCode] = useState('')
  const join = useMutation({
    mutationFn: () => joinDuel(code.trim()),
    onSuccess: (result) => navigate(`/contests/${result.contest.id}`),
  })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    join.mutate()
  }

  return (
    <section aria-labelledby="duel-heading" className={styles.duel}>
      <div>
        <h2 id="duel-heading" className={styles.sectionTitle}>
          미니 대결
        </h2>
        <p className={styles.muted}>짧은 문제를 둘이 동시에 풉니다. 여는 것은 풀이 화면의 "미니 대결"에서, 받은 코드로는 여기서 붙습니다.</p>
      </div>
      <form className={styles.duelForm} onSubmit={submit}>
        <TextField
          label="받은 코드"
          value={code}
          onChange={(event) => setCode(event.target.value.toUpperCase())}
          maxLength={6}
          autoComplete="off"
          spellCheck={false}
          error={join.error instanceof Error ? join.error.message : undefined}
        />
        <Button type="submit" variant="primary" loading={join.isPending} disabled={code.trim().length !== 6}>
          코드로 붙기
        </Button>
      </form>
    </section>
  )
}
