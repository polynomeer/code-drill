import { ChevronRight, ClipboardList, Home, LogOut, Monitor, Moon, Settings, Sun, Trophy, UserRound } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'wouter'
import { logout } from '../../api/client'
import { useSession } from '../../api/session'
import { OPEN_SETTINGS } from '../../app/AppShell'
import { getThemePreference, setThemePreference } from '../../design'
import type { ThemePreference } from '../../design'
import styles from './MePage.module.css'

const THEMES: { value: ThemePreference; label: string; Icon: typeof Sun }[] = [
  { value: 'light', label: '라이트', Icon: Sun },
  { value: 'dark', label: '다크', Icon: Moon },
  { value: 'system', label: '시스템', Icon: Monitor },
]

/**
 * 나 `/me` — 모바일 하단 탭의 넷째 칸 (docs/ui-overhaul.md §4).
 *
 * 탭은 넷뿐이라(문제·훈련·역량·나) 나머지는 여기서 간다: 홈의 오늘 요약, 대회, 제출, 프로필, 설정, 테마,
 * 로그아웃. 데스크톱에서는 위 메뉴와 사용자 메뉴가 같은 일을 한다 — 이 화면은 주소로 열면 그대로 쓸 수 있다.
 */
export function MePage() {
  const session = useSession()
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference)
  if (!session) return null

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <span className={styles.avatar} aria-hidden="true">
          {session.displayName.slice(0, 1)}
        </span>
        <h1 className={styles.name}>{session.displayName}</h1>
      </header>

      <nav aria-label="내 메뉴">
        <ul className={styles.list}>
          <Row href="/" icon={<Home size={18} />}>
            오늘 — 처방 요약·프로젝트·문제집
          </Row>
          <Row href="/contests" icon={<Trophy size={18} />}>
            대회
          </Row>
          <Row href="/submissions" icon={<ClipboardList size={18} />}>
            내 제출
          </Row>
          <Row href="/u/me" icon={<UserRound size={18} />}>
            내 프로필
          </Row>
          <li>
            <button type="button" className={styles.row} onClick={() => window.dispatchEvent(new Event(OPEN_SETTINGS))}>
              <Settings size={18} aria-hidden="true" />
              <span className={styles.label}>계정 설정</span>
              <ChevronRight size={16} aria-hidden="true" className={styles.chevron} />
            </button>
          </li>
        </ul>
      </nav>

      <fieldset className={styles.themes}>
        <legend className={styles.legend}>테마</legend>
        <div className={styles.segmented}>
          {THEMES.map(({ value, label, Icon }) => (
            <button
              key={value}
              type="button"
              aria-pressed={theme === value}
              className={theme === value ? `${styles.segment} ${styles.segmentOn}` : styles.segment}
              onClick={() => {
                setThemePreference(value)
                setTheme(value)
              }}
            >
              <Icon size={16} aria-hidden="true" />
              {label}
            </button>
          ))}
        </div>
      </fieldset>

      <button type="button" className={styles.logout} onClick={() => void logout()}>
        <LogOut size={18} aria-hidden="true" />
        로그아웃
      </button>
    </div>
  )
}

function Row({ href, icon, children }: { href: string; icon: ReactNode; children: ReactNode }) {
  return (
    <li>
      <Link href={href} className={styles.row}>
        <span aria-hidden="true">{icon}</span>
        <span className={styles.label}>{children}</span>
        <ChevronRight size={16} aria-hidden="true" className={styles.chevron} />
      </Link>
    </li>
  )
}
