import type { ReactNode } from 'react'
import { Link } from 'wouter'
import styles from './AuthLayout.module.css'

/** 로그인·가입·재설정이 함께 쓰는 틀 — 가운데 카드 하나, 위에 이름과 한 줄. */
export function AuthLayout({ title, lead, children, footer }: { title: string; lead?: ReactNode; children: ReactNode; footer?: ReactNode }) {
  return (
    <main className={styles.page} id="main">
      <div className={styles.card}>
        <Link href="/problems" className={styles.brand} aria-label="CodeDrill — 문제 둘러보기">
          <span className={styles.logo} aria-hidden="true">
            CD
          </span>
          CodeDrill
        </Link>
        <h1 className={styles.title}>{title}</h1>
        {lead && <p className={styles.lead}>{lead}</p>}
        {children}
      </div>
      {footer && <div className={styles.footer}>{footer}</div>}
    </main>
  )
}
