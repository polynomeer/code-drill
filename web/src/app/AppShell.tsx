import { ChevronDown, Dumbbell, LayoutList, LogOut, Monitor, Moon, Radar, Search, Settings, Sun, UserRound } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useLocation } from 'wouter'
import { logout } from '../api/client'
import type { Session } from '../api/session'
import { Dialog, getThemePreference, setThemePreference } from '../design'
import type { ThemePreference } from '../design'
import { AccountSettings } from '../features/auth/AccountSettings'
import { SanctionBanner } from '../features/auth/SanctionBanner'
import { NotificationBell } from '../features/notifications/NotificationBell'
// ⌘K 팔레트는 첫 화면과 함께 온다(2KB). 늦게 받아 오면 첫 ⌘K 직후에 친 글자가 그 사이에 사라진다.
// 찾을 문제 목록은 처음 열 때 받는다
import CommandPalette from '../features/search/CommandPalette'
import type { Command } from '../features/search/search'
import styles from './AppShell.module.css'
import { markOpenSource } from '../shared/analytics'


/** 모바일 하단 탭 (ui-overhaul.md §4 "하단 탭 4개(문제·훈련·역량·나)"). 나머지는 "나"에서 간다 */
const TABS = [
  { to: '/problems', label: '문제', Icon: LayoutList, match: ['/problems'] },
  { to: '/training', label: '훈련', Icon: Dumbbell, match: ['/training', '/welcome'] },
  { to: '/competencies', label: '역량', Icon: Radar, match: ['/competencies'] },
  { to: '/me', label: '나', Icon: UserRound, match: ['/me', '/u/', '/submissions', '/contests', '/admin'] },
]

/** 다른 화면이 계정 설정 대화상자를 열 때 보내는 신호 ("나" 화면의 계정 설정) */
export const OPEN_SETTINGS = 'codedrill:open-settings'

/**
 * 전역 앱 셸 (디자인 설계서 §2.2 전역 내비게이션).
 *
 * 내비게이션에는 **지금 있는 라우트만** 올린다. 훈련·역량·대회는 ui-overhaul.md 의 단계가
 * 라우트를 열 때 여기 한 줄씩 늘어난다 — 누르면 같은 화면이 나오는 메뉴는 메뉴가 아니다.
 */
const NAV = [
  { to: '/', label: '홈', signedIn: true },
  { to: '/problems', label: '문제', signedIn: false },
  { to: '/training', label: '훈련', signedIn: true },
  { to: '/competencies', label: '역량', signedIn: true },
  { to: '/contests', label: '대회', signedIn: true },
  { to: '/submissions', label: '제출', signedIn: true },
]

const THEME_OPTIONS: { value: ThemePreference; label: string; Icon: typeof Sun }[] = [
  { value: 'light', label: '라이트', Icon: Sun },
  { value: 'dark', label: '다크', Icon: Moon },
  { value: 'system', label: '시스템 설정', Icon: Monitor },
]

export function AppShell({
  session,
  immersive = false,
  children,
}: {
  /** 없으면 둘러보는 사람이다 — 공개 화면만 열리고 오른쪽에 로그인이 있다 */
  session: Session | null
  /** 풀이 화면처럼 화면 전체를 쓰는 곳. 전역 헤더를 접고 본문이 남은 높이를 다 갖는다 */
  immersive?: boolean
  children: ReactNode
}) {
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [location, navigate] = useLocation()
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference)
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [paletteLoaded, setPaletteLoaded] = useState(false)

  const chooseTheme = (value: ThemePreference) => {
    setThemePreference(value)
    setTheme(value)
  }

  const openPalette = () => {
    setPaletteLoaded(true)
    setPaletteOpen(true)
  }

  // ⌘K / Ctrl+K 는 어디서든 (편집기 안에서도 — 풀이 중에 다른 문제로 가는 길이다). `/` 는 글을 쓰는 중이 아닐 때만
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      const typing = !!target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))
      if ((event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        event.stopPropagation()
        openPalette()
      } else if (event.key === '/' && !typing && !event.metaKey && !event.ctrlKey && !event.altKey) {
        event.preventDefault()
        openPalette()
      }
    }
    window.addEventListener('keydown', onKey, { capture: true })
    return () => window.removeEventListener('keydown', onKey, { capture: true })
  }, [])

  useEffect(() => {
    const open = () => setSettingsOpen(true)
    window.addEventListener(OPEN_SETTINGS, open)
    return () => window.removeEventListener(OPEN_SETTINGS, open)
  }, [])

  const commands = useMemo<Command[]>(() => {
    const go = (to: string) => () => navigate(to)
    const pages: Command[] = [
      { id: 'problems', label: '문제 목록', keywords: ['problems', '목록'], group: '이동', run: go('/problems') },
      ...(session
        ? [
            { id: 'home', label: '홈', keywords: ['home', '오늘'], group: '이동' as const, run: go('/') },
            { id: 'training', label: '오늘의 훈련', keywords: ['training', '처방', '훈련'], group: '이동' as const, run: go('/training') },
            { id: 'competencies', label: '역량 지도', keywords: ['competency', '역량', '근거'], group: '이동' as const, run: go('/competencies') },
            { id: 'weekly', label: '이번 주 리포트', keywords: ['weekly', 'report', '리포트'], group: '이동' as const, run: go('/competencies?tab=weekly') },
            { id: 'contests', label: '대회', keywords: ['contest', '대결'], group: '이동' as const, run: go('/contests') },
            { id: 'submissions', label: '내 제출', keywords: ['submissions', '제출'], group: '이동' as const, run: go('/submissions') },
            { id: 'profile', label: '내 프로필', keywords: ['profile', '프로필'], group: '이동' as const, run: go('/u/me') },
            { id: 'settings', label: '계정 설정', keywords: ['settings', '비밀번호', '설정'], group: '행동' as const, run: () => setSettingsOpen(true) },
          ]
        : [{ id: 'login', label: '로그인 · 가입', keywords: ['login', 'sign in', '가입'], group: '이동' as const, run: go(`/login?next=${encodeURIComponent(location)}`) }]),
    ]
    const themes: Command[] = THEME_OPTIONS.map(({ value, label }) => ({
      id: `theme-${value}`,
      label: `테마: ${label}`,
      keywords: ['theme', '테마', value],
      group: '행동',
      run: () => chooseTheme(value),
    }))
    const signOut: Command[] = session ? [{ id: 'logout', label: '로그아웃', keywords: ['logout', 'sign out'], group: '행동', run: () => void logout() }] : []
    return [...pages, ...themes, ...signOut]
  }, [session, location, navigate])

  return (
    <div className={[styles.shell, immersive ? styles.immersive : '', session ? styles.member : ''].join(' ')}>
      <a className="skip-link" href="#main">
        본문으로 건너뛰기
      </a>
      <header className={styles.header} hidden={immersive}>
        <div className={styles.headerInner}>
          <Link href="/" className={styles.logo} aria-label="CodeDrill 홈">
            <span className={styles.logoMark} aria-hidden="true">
              CD
            </span>
            <span className={styles.logoText}>CodeDrill</span>
          </Link>

          <nav aria-label="주 메뉴" className={styles.nav}>
            {NAV.filter((item) => session || !item.signedIn).map((item) => (
              <Link
                key={item.to}
                href={item.to}
                className={isCurrent(location, item.to) ? `${styles.navItem} ${styles.navActive}` : styles.navItem}
                aria-current={isCurrent(location, item.to) ? 'page' : undefined}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className={styles.userArea}>
            <button
              type="button"
              className={styles.search}
              onClick={openPalette}
              aria-label="검색 (⌘K)"
              aria-keyshortcuts="Meta+K Control+K"
            >
              <Search size={16} aria-hidden="true" />
              <span className={styles.searchLabel} aria-hidden="true">
                검색
              </span>
              <kbd className={styles.searchKey} aria-hidden="true">
                ⌘K
              </kbd>
            </button>
            {!session ? (
              <Link href={`/login?next=${encodeURIComponent(location)}`} className={styles.signIn}>
                로그인
              </Link>
            ) : (
            <>
            <NotificationBell />
            {/* Popover API — 바깥 클릭과 Esc 로 닫히는 것을 브라우저가 준다 */}
            <button
              type="button"
              className={styles.userButton}
              popoverTarget="user-menu"
              // 좁은 화면에서는 이름 글자가 숨는다 — 그때도 버튼 이름은 남아야 한다
              aria-label={`${session.displayName} 계정 메뉴`}
            >
              <span className={styles.avatar} aria-hidden="true">
                {session.displayName.slice(0, 1)}
              </span>
              <span className={styles.userName}>{session.displayName}</span>
              <ChevronDown size={14} aria-hidden="true" />
            </button>
            <div id="user-menu" popover="auto" className={styles.menu}>
              <p className={styles.menuHeading}>{session.displayName}</p>
              <Link
                href="/u/me"
                className={styles.menuItem}
                onClick={() => document.getElementById('user-menu')?.hidePopover?.()}
              >
                <UserRound size={16} aria-hidden="true" />
                내 프로필
              </Link>
              <button
                type="button"
                className={styles.menuItem}
                popoverTarget="user-menu"
                popoverTargetAction="hide"
                onClick={() => setSettingsOpen(true)}
              >
                <Settings size={16} aria-hidden="true" />
                계정 설정
              </button>

              <fieldset className={styles.menuGroup}>
                <legend className={styles.menuLabel}>테마</legend>
                {THEME_OPTIONS.map(({ value, label, Icon }) => (
                  <button
                    key={value}
                    type="button"
                    className={styles.menuItem}
                    aria-pressed={theme === value}
                    onClick={() => chooseTheme(value)}
                  >
                    <Icon size={16} aria-hidden="true" />
                    {label}
                  </button>
                ))}
              </fieldset>

              <button type="button" className={styles.menuItem} onClick={() => void logout()}>
                <LogOut size={16} aria-hidden="true" />
                로그아웃
              </button>
            </div>
            </>
            )}
          </div>
        </div>
      </header>

      {/* 제재는 무엇보다 먼저 보여야 한다 (§8.5). 없으면 아무것도 그리지 않는다. */}
      {session && <SanctionBanner />}

      <main id="main" tabIndex={-1} className={styles.main}>
        {children}
      </main>

      {session && !immersive && (
        <nav aria-label="하단 메뉴" className={styles.tabBar}>
          {TABS.map(({ to, label, Icon, match }) => {
            const current = match.some((prefix) =>
              prefix.endsWith('/') ? location.startsWith(prefix) : location === prefix || location.startsWith(`${prefix}/`),
            )
            return (
              <Link key={to} href={to} className={current ? `${styles.tab} ${styles.tabActive}` : styles.tab} aria-current={current ? 'page' : undefined}>
                <Icon size={20} aria-hidden="true" />
                {label}
              </Link>
            )
          })}
        </nav>
      )}

      {paletteLoaded && (
          <CommandPalette
            open={paletteOpen}
            onClose={() => setPaletteOpen(false)}
            commands={commands}
            onOpenProblem={(id) => {
              markOpenSource('search')
              navigate(`/problems/${id}/solve`)
            }}
          />
      )}

      <Dialog
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        title="계정 설정"
        description="표시 이름, 비밀번호, 내 데이터"
        size="wide"
      >
        {settingsOpen && session && <AccountSettings session={session} />}
      </Dialog>
    </div>
  )
}

/** 홈은 정확히 `/` 일 때만, 나머지는 그 아래 경로까지 현재 메뉴다 (`/problems/x` 도 "문제"). */
function isCurrent(location: string, to: string): boolean {
  return to === '/' ? location === '/' : location === to || location.startsWith(`${to}/`)
}
