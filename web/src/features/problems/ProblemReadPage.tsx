import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Code } from 'lucide-react'
import { useEffect, useMemo } from 'react'
import { Link } from 'wouter'
import { getProblem } from '../../api/client'
import { useSession } from '../../api/session'
import { EmptyState, Skeleton } from '../../design'
import { StatementView } from '../workspace/StatementView'
import { splitStatement } from '../workspace/statement'
import styles from './ProblemReadPage.module.css'
import { markOpenSource } from '../../shared/analytics'

/**
 * 문제 읽기 `/problems/:slug` — 로그인 없이 열리는 공개 본문 (디자인 설계서 §2.3, §11.1).
 *
 * 둘러보는 사람이 문제를 읽고 "풀어 볼까"를 정하는 자리다. 풀기는 로그인 뒤 풀이 화면에서 한다 —
 * 로그인하고 나면 이 문제의 풀이 화면으로 바로 돌아온다 (`?next=`).
 *
 * 로그인 없이 코드를 쓰기 시작하는 것(로컬 초안과 병합)은 첫 경험 단계(U8)의 일이다.
 */
export function ProblemReadPage({ slug }: { slug: string }) {
  const session = useSession()
  const query = useQuery({ queryKey: ['problem', slug], queryFn: () => getProblem(slug) })
  const problem = query.data
  const body = useMemo(() => (problem ? splitStatement(problem.statement, problem.title).body : ''), [problem])

  useEffect(() => {
    if (problem) document.title = `${problem.number ? `${problem.number}. ` : ''}${problem.title} · CodeDrill`
    return () => {
      document.title = 'CodeDrill'
    }
  }, [problem])

  // 로그인하지 않아도 풀이 화면에서 코드를 쓰기 시작할 수 있다 — 실행·제출할 때 로그인한다 (§6.9)
  const solve = `/problems/${slug}/solve`

  if (query.isError) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="문제를 찾지 못했습니다"
          action={
            <Link href="/problems" className="linklike">
              문제 목록으로
            </Link>
          }
        >
          주소가 바뀌었거나 공개되지 않은 문제입니다.
        </EmptyState>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <Link href="/problems" className={styles.back}>
        <ArrowLeft size={16} aria-hidden="true" />
        문제 목록
      </Link>

      <header className={styles.head}>
        {problem ? (
          <h1 className={styles.title}>
            {problem.number && <span className={styles.number}>{problem.number}</span>}
            {problem.title}
          </h1>
        ) : (
          <Skeleton width="50%" height={30} />
        )}
        <Link href={solve} className={styles.solve} onClick={() => markOpenSource('link')}>
          <Code size={16} aria-hidden="true" />
          {session ? '풀기' : '풀어 보기'}
        </Link>
      </header>

      {problem ? (
        <StatementView problem={problem} body={body} />
      ) : (
        <div role="status" aria-busy="true" className={styles.loading}>
          <span className="visually-hidden">문제를 불러오는 중</span>
          <Skeleton />
          <Skeleton width="85%" />
          <Skeleton width="70%" />
        </div>
      )}
    </div>
  )
}
