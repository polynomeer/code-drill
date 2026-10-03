import { ChevronDown, LogOut, Monitor, Moon, Settings, Sun } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useLocation } from 'wouter'
import { logout } from '../api/client'
import type { Session } from '../api/session'
import { Dialog, getThemePreference, setThemePreference } from '../design'
import type { ThemePreference } from '../design'
import { AccountSettings } from '../features/auth/AccountSettings'
import { SanctionBanner } from '../features/auth/SanctionBanner'
import styles from './AppShell.module.css'

/**
 * 전역 앱 셸 (디자인 설계서 §2.2 전역 내비게이션).
 *
 * 내비게이션에는 **지금 있는 라우트만** 올린다. 훈련·역량·대회는 ui-overhaul.md 의 단계가
 * 라우트를 열 때 여기 한 줄씩 늘어난다 — 누르면 같은 화면이 나오는 메뉴는 메뉴가 아니다.
 */
const NAV = [{ to: '/', label: '문제' }]

const THEME_OPTIONS: { value: ThemePreference; label: string; Icon: typeof Sun }[] = [
  { value: 'light', label: '라이트', Icon: Sun },
  { value: 'dark', label: '다크', Icon: Moon },
  { value: 'system', label: '시스템 설정', Icon: Monitor },
]

export function AppShell({ session, children }: { session: Session; children: ReactNode }) {
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [location] = useLocation()
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference)

  const chooseTheme = (value: ThemePreference) => {
    setThemePreference(value)
    setTheme(value)
  }

  return (
    <div className={styles.shell}>
      <a className="skip-link" href="#main">
        본문으로 건너뛰기
      </a>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link href="/" className={styles.logo} aria-label="CodeDrill 홈">
            <span className={styles.logoMark} aria-hidden="true">
              CD
            </span>
            <span className={styles.logoText}>CodeDrill</span>
          </Link>

          <nav aria-label="주 메뉴" className={styles.nav}>
            {NAV.map((item) => (
              <Link
                key={item.to}
                href={item.to}
                className={(active) => (active ? `${styles.navItem} ${styles.navActive}` : styles.navItem)}
                aria-current={location === item.to ? 'page' : undefined}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className={styles.userArea}>
            {/* Popover API — 바깥 클릭과 Esc 로 닫히는 것을 브라우저가 준다 */}
            <button type="button" className={styles.userButton} popoverTarget="user-menu">
              <span className={styles.avatar} aria-hidden="true">
                {session.displayName.slice(0, 1)}
              </span>
              <span className={styles.userName}>{session.displayName}</span>
              <ChevronDown size={14} aria-hidden="true" />
            </button>
            <div id="user-menu" popover="auto" className={styles.menu}>
              <p className={styles.menuHeading}>{session.displayName}</p>
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
          </div>
        </div>
      </header>

      {/* 제재는 무엇보다 먼저 보여야 한다 (§8.5). 없으면 아무것도 그리지 않는다. */}
      <SanctionBanner />

      <main id="main" tabIndex={-1} className={styles.main}>
        {children}
      </main>

      <Dialog
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        title="계정 설정"
        description="표시 이름, 비밀번호, 내 데이터"
        size="wide"
      >
        {settingsOpen && <AccountSettings session={session} />}
      </Dialog>
    </div>
  )
}
