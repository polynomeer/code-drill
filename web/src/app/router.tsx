import { Suspense, lazy } from 'react'
import type { ReactNode } from 'react'
import { Link, Redirect, Route, Switch, useLocation, useRoute } from 'wouter'
import { useSession } from '../api/session'
import { EmptyState, Skeleton } from '../design'
import { SignIn } from '../features/auth/SignIn'
import { ProblemsPage } from '../features/problems/ProblemsPage'
import { AppShell } from './AppShell'
import { safeNext } from './navigation'
import { HomePage } from './HomePage'

/**
 * 라우트 표 (docs/ui-overhaul.md §4).
 *
 * **공개와 로그인 필요를 라우트마다 정한다.** 문제 목록과 읽기는 로그인 없이 열리고
 * (디자인 설계서 §11.1), 풀이·제출·홈은 로그인해야 한다. 로그인이 필요한 곳에 둘러보던 사람이
 * 오면 `/login?next=<원래 주소>` 로 보내고, 로그인하면 그 주소로 돌아온다.
 *
 * 풀이·읽기 화면은 Monaco·마크다운 렌더러를 끌고 오므로 따로 청크로 나눈다.
 *
 * 쿼리(`?submission=`·`?step=`·`?lang=`·필터)는 각 화면이 history.replaceState 로 직접 쓴다
 * (shared/url.ts). wouter 는 경로만 보므로 둘이 부딪히지 않는다.
 */

// 컴포넌트 카탈로그 (ui-overhaul.md §5.2). 로그인 없이 열리고, 배포 빌드에서는 import 째로 빠진다.
const DesignPage = import.meta.env.DEV
  ? lazy(() => import('../design/DesignPage').then((m) => ({ default: m.DesignPage })))
  : null

const SolvePage = lazy(() => import('../features/workspace/SolvePage').then((m) => ({ default: m.SolvePage })))
const SubmissionsPage = lazy(() =>
  import('../features/submissions/SubmissionsPage').then((m) => ({ default: m.SubmissionsPage })),
)
const SubmissionDetailPage = lazy(() =>
  import('../features/submissions/SubmissionDetailPage').then((m) => ({ default: m.SubmissionDetailPage })),
)
const ComparePage = lazy(() => import('../features/submissions/ComparePage').then((m) => ({ default: m.ComparePage })))
const ReplayPage = lazy(() => import('../features/replay/ReplayPage').then((m) => ({ default: m.ReplayPage })))
const TrainingPage = lazy(() => import('../features/training/TrainingPage').then((m) => ({ default: m.TrainingPage })))
const CompetenciesPage = lazy(() =>
  import('../features/competency/CompetenciesPage').then((m) => ({ default: m.CompetenciesPage })),
)
const ProblemReadPage = lazy(() =>
  import('../features/problems/ProblemReadPage').then((m) => ({ default: m.ProblemReadPage })),
)

const SOLVE_PATH = '/problems/:slug/solve'
const REPLAY_PATH = '/submissions/:id/replay'

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
      <Route path="/login">
        <LoginPage />
      </Route>
      <Route>
        <Shell />
      </Route>
    </Switch>
  )
}

function Shell() {
  const session = useSession()
  // 풀이·리플레이 화면은 전역 헤더를 접고 자기 툴바만 쓴다 (ui-overhaul.md §4).
  const [solving] = useRoute(SOLVE_PATH)
  const [replaying] = useRoute(REPLAY_PATH)

  return (
    // 계정이 바뀌면 셸 아래 상태를 통째로 버린다. 앞 사람의 초안이 남으면 안 된다.
    <AppShell key={session?.userId ?? 'anonymous'} session={session} immersive={(solving || replaying) && session !== null}>
      <Switch>
        <Route path="/">
          {session ? (
            <LegacySubmissionLink>
              <HomePage />
            </LegacySubmissionLink>
          ) : (
            // 둘러보는 사람의 첫 화면은 문제 목록이다. 예전 제출 링크는 로그인으로 보낸다.
            <LegacySubmissionLink>
              <Redirect to="/problems" replace />
            </LegacySubmissionLink>
          )}
        </Route>
        <Route path="/problems">
          <ProblemsPage />
        </Route>
        <Route path={SOLVE_PATH}>
          {(params) => (
            <RequireSession>
              <Suspense fallback={<RouteLoading />}>
                {/* 문제를 바꾸면 화면 상태를 통째로 새로 시작한다 — 앞 문제의 판정·초안이 남지 않게 */}
                <SolvePage key={params.slug} slug={params.slug} />
              </Suspense>
            </RequireSession>
          )}
        </Route>
        <Route path="/problems/:slug">
          {(params) => (
            <Suspense fallback={<RouteLoading />}>
              <ProblemReadPage key={params.slug} slug={params.slug} />
            </Suspense>
          )}
        </Route>
        <Route path="/training">
          <RequireSession>
            <Suspense fallback={<RouteLoading />}>
              <TrainingPage />
            </Suspense>
          </RequireSession>
        </Route>
        <Route path="/competencies">
          <RequireSession>
            <Suspense fallback={<RouteLoading />}>
              <CompetenciesPage />
            </Suspense>
          </RequireSession>
        </Route>
        <Route path="/submissions">
          <RequireSession>
            <Suspense fallback={<RouteLoading />}>
              <SubmissionsPage />
            </Suspense>
          </RequireSession>
        </Route>
        <Route path="/submissions/compare">
          <RequireSession>
            <Suspense fallback={<RouteLoading />}>
              <ComparePage />
            </Suspense>
          </RequireSession>
        </Route>
        <Route path={REPLAY_PATH}>
          {(params) => (
            <RequireSession>
              <Suspense fallback={<RouteLoading />}>
                <ReplayPage key={params.id} id={params.id} />
              </Suspense>
            </RequireSession>
          )}
        </Route>
        <Route path="/submissions/:id">
          {(params) => (
            <RequireSession>
              <Suspense fallback={<RouteLoading />}>
                <SubmissionDetailPage key={params.id} id={params.id} />
              </Suspense>
            </RequireSession>
          )}
        </Route>
        <Route>
          <NotFound />
        </Route>
      </Switch>
    </AppShell>
  )
}

/** 로그인해야 하는 화면. 아니면 지금 주소를 들고 로그인으로 간다. */
function RequireSession({ children }: { children: ReactNode }) {
  const session = useSession()
  if (session) return <>{children}</>
  const here = window.location.pathname + window.location.search
  return <Redirect to={`/login?next=${encodeURIComponent(here)}`} replace />
}

/** `/login?next=` — 로그인하면 원래 가려던 곳으로 돌아간다. 밖으로 나가는 주소는 safeNext 가 막는다. */
function LoginPage() {
  const session = useSession()
  const [, navigate] = useLocation()
  const next = safeNext(new URLSearchParams(window.location.search).get('next'))
  if (session) return <Redirect to={next} replace />
  return <SignIn onSignedIn={() => navigate(next, { replace: true })} />
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
          <Link href="/problems" className="linklike">
            문제 목록으로
          </Link>
        }
      >
        주소가 바뀌었거나 아직 없는 화면입니다.
      </EmptyState>
    </div>
  )
}
