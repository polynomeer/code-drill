import { Lock } from 'lucide-react'
import { VerdictBadge } from '../../design'
import type { CaseRow } from './caseGridModel'
import styles from './CaseGrid.module.css'

/**
 * 케이스 격자 (docs/ui-overhaul.md §6.3) — 그룹마다 한 줄, 케이스마다 한 칸.
 *
 * 칸의 뜻은 색만이 아니라 무늬로도 갈린다: 통과는 꽉 찬 칸, 실패는 빗금, 제한 초과는 가로줄,
 * 아직은 점선 테두리 (디자인 설계서 §15.2). 줄마다 같은 내용을 글로도 적는다.
 */
export function CaseGrid({ rows, judging = false }: { rows: CaseRow[]; judging?: boolean }) {
  return (
    <section className={styles.grid} aria-label="테스트 결과">
      <div className={styles.head}>
        <h3 className={styles.title}>테스트 결과</h3>
        {judging && <span className={styles.status}>채점 중</span>}
      </div>

      <table className={styles.rows}>
        <caption className="visually-hidden">그룹별 결과</caption>
        <thead>
          <tr>
            <th scope="col">그룹</th>
            <th scope="col">케이스</th>
            <th scope="col">판정</th>
            <th scope="col">점수</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.groupId}>
              <th scope="row">
                <code>{row.groupId}</code>
                {row.hidden && <Lock size={12} aria-label="숨은 그룹" className={styles.lock} />}
              </th>
              <td>
                <div className={styles.cells} role="img" aria-label={row.label}>
                  {row.cells.map((cell, index) => (
                    <span
                      key={index}
                      className={[styles.cell, styles[cell.state], cell.first ? styles.first : ''].join(' ')}
                      title={cell.caseId ? `케이스 ${cell.caseId}` : undefined}
                    />
                  ))}
                  {row.folded > 0 && <span className={styles.folded}>+{row.folded}</span>}
                </div>
                <span className={styles.label} aria-hidden="true">
                  {row.label}
                </span>
              </td>
              <td>{row.verdict ? <VerdictBadge verdict={row.verdict} /> : <span className={styles.label}>—</span>}</td>
              <td className={styles.num}>{row.maxScore > 0 ? (row.verdict ? `${row.score} / ${row.maxScore}` : `${row.maxScore}점`) : '예제'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
