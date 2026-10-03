import { Suspense, lazy, useEffect, useState } from 'react'
import { Link, Route, Switch } from 'wouter'
import { getSession, onSessionChange } from '../api/session'
import type { Session } from '../api/session'
import { Drill } from '../App'
import { EmptyState } from '../design'
import { SignIn } from '../features/auth/SignIn'
import { AppShell } from './AppShell'

/**
 * 라우트 표 (docs/ui-overhaul.md §4).
 *
 * 화면이 하나씩 자기 라우트를 얻을 때마다 여기 한 줄이 는다. 지금은 기존 한 화면이 `/` 에
 * 그대로 들어 있다 — U0 은 자리만 만들고 화면을 옮기지 않는다.
 *
 * 기존 화면은 `?submission=`·`?step=`·필터를 history.replaceState 로 직접 쓴다
 * (shared/url.ts). wouter 는 경로만 보고 쿼리는 화면에 맡기므로 둘이 부딪히지 않는다.
 */

// 컴포넌트 카탈로그 (ui-overhaul.md §5.2). 로그인 없이 열리고, 배포 빌드에서는 import 째로 빠진다.
const DesignPage = import.meta.env.DEV
  ? lazy(() => import('../design/DesignPage').then((m) => ({ default: m.DesignPage })))
  : null

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
  if (!session) return <SignIn onSignedIn={setSession} />
  // 계정이 바뀌면 셸 아래 상태를 통째로 버린다. 앞 사람의 초안이 남으면 안 된다.
  return (
    <AppShell key={session.userId} session={session}>
      <Switch>
        <Route path="/">
          <Drill />
        </Route>
        <Route>
          <NotFound />
        </Route>
      </Switch>
    </AppShell>
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
