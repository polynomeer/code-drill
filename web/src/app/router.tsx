import { useQuery } from '@tanstack/react-query'
import { Suspense, lazy, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, Redirect, Route, Switch, useRoute } from 'wouter'
import { getSubmission } from '../api/client'
import { getSession, onSessionChange } from '../api/session'
import type { Session } from '../api/session'
import { EmptyState, Skeleton } from '../design'
import { SignIn } from '../features/auth/SignIn'
import { AppShell } from './AppShell'
import { HomePage } from './HomePage'

/**
 * 라우트 표 (docs/ui-overhaul.md §4).
 *
 * 화면이 하나씩 자기 라우트를 얻을 때마다 여기 한 줄이 는다. 풀이 화면은 Monaco 와 마크다운
 * 렌더러를 끌고 오므로 따로 청크로 나눈다 — 홈은 그것을 기다리지 않는다.
 *
 * 쿼리(`?submission=`·`?step=`·`?lang=`·필터)는 각 화면이 history.replaceState 로 직접 쓴다
 * (shared/url.ts). wouter 는 경로만 보므로 둘이 부딪히지 않는다.
 */

// 컴포넌트 카탈로그 (ui-overhaul.md §5.2). 로그인 없이 열리고, 배포 빌드에서는 import 째로 빠진다.
const DesignPage = import.meta.env.DEV
  ? lazy(() => import('../design/DesignPage').then((m) => ({ default: m.DesignPage })))
  : null

const SolvePage = lazy(() => import('../features/workspace/SolvePage').then((m) => ({ default: m.SolvePage })))

const SOLVE_PATH = '/problems/:slug/solve'

export function AppRoutes() {
  return (
    <Switch>
      {DesignPage && (
        <Route path="/design">
          <Suspense fallback={null}>
            <DesignPage />
          </Suspense>
        </Route>
      )}
      <Route>
        <SessionGate />
      </Route>
    </Switch>
  )
}

/**
 * 세션이 없으면 로그인 화면이다. 제출·초안·기록이 전부 인증을 요구하므로, 로그인 전 화면은
 * 실패한 요청 목록이 될 뿐이다. (익명 탐색은 U2 에서 백엔드와 함께 연다.)
 *
 * 토큰 갱신이 끝내 실패하면 세션 모듈이 스스로 비운다. 그때 화면도 로그인으로 돌아가야
 * 한다 — 그러지 않으면 사용자는 아무 반응 없는 화면을 보게 된다.
 */
function SessionGate() {
  const [session, setSession] = useState<Session | null>(getSession)
  useEffect(() => onSessionChange(setSession), [])
  // 풀이 화면은 전역 헤더를 접고 자기 툴바만 쓴다 (ui-overhaul.md §4).
  const [solving] = useRoute(SOLVE_PATH)

  if (!session) return <SignIn onSignedIn={setSession} />
  // 계정이 바뀌면 셸 아래 상태를 통째로 버린다. 앞 사람의 초안이 남으면 안 된다.
  return (
    <AppShell key={session.userId} session={session} immersive={solving}>
      <Switch>
        <Route path="/">
          <LegacySubmissionLink>
            <HomePage />
          </LegacySubmissionLink>
        </Route>
        <Route path={SOLVE_PATH}>
          {(params) => (
            <Suspense fallback={<RouteLoading />}>
              {/* 문제를 바꾸면 화면 상태를 통째로 새로 시작한다 — 앞 문제의 판정·초안이 남지 않게 */}
              <SolvePage key={params.slug} slug={params.slug} />
            </Suspense>
          )}
        </Route>
        <Route path="/submissions/:id">{(params) => <SubmissionLink id={params.id} />}</Route>
        <Route>
          <NotFound />
        </Route>
      </Switch>
    </AppShell>
  )
}

/**
 * `/submissions/:id` — 제출 하나로 가는 링크 (역량 근거, 게시판 붙임).
 *
 * 제출 상세 화면은 U3 에서 선다. 그때까지는 그 제출의 문제를 풀이 화면으로 열고 제출과 걸음을
 * 넘긴다. 링크를 이 모양으로 먼저 굳혀 두면 U3 에서 바꿀 것은 이 컴포넌트뿐이다.
 */
function SubmissionLink({ id }: { id: string }) {
  const query = useQuery({ queryKey: ['submission', id], queryFn: () => getSubmission(id) })
  if (query.isError) return <NotFound />
  if (!query.data) return <RouteLoading />
  const params = new URLSearchParams({ submission: id })
  const step = new URLSearchParams(window.location.search).get('step')
  if (step !== null) params.set('step', step)
  return <Redirect to={`/problems/${query.data.problemId}/solve?${params}`} replace />
}

/**
 * U1 전에는 제출을 `/?submission=<id>&step=<n>` 으로 열었다. 그 링크(북마크·게시판에 남은
 * 주소)를 새 주소로 넘긴다.
 */
function LegacySubmissionLink({ children }: { children: ReactNode }) {
  const params = new URLSearchParams(window.location.search)
  const id = params.get('submission')
  if (!id) return <>{children}</>
  const step = params.get('step')
  return <Redirect to={`/submissions/${id}${step === null ? '' : `?step=${step}`}`} replace />
}

function RouteLoading() {
  return (
    <div style={{ padding: 'var(--space-5)', display: 'grid', gap: 'var(--space-2)' }} role="status" aria-busy="true">
      <span className="visually-hidden">불러오는 중</span>
      <Skeleton width="30%" height={20} />
      <Skeleton />
      <Skeleton width="80%" />
    </div>
  )
}

function NotFound() {
  return (
    <div style={{ padding: 'var(--space-7) var(--space-4)' }}>
      <EmptyState
        title="찾는 화면이 없습니다"
        action={
          <Link href="/" className="linklike">
            문제 목록으로
          </Link>
        }
      >
        주소가 바뀌었거나 아직 없는 화면입니다.
      </EmptyState>
    </div>
  )
}
