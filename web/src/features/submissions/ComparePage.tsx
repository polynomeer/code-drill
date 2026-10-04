import { useQueries } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Suspense, lazy } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'wouter'
import { getSubmission, getSubmissionSource } from '../../api/client'
import { EmptyState, JudgeStatusBadge, Skeleton, VerdictBadge } from '../../design'
import { EDITOR_LANGUAGE, IN_FLIGHT, LANGUAGE_LABEL } from '../../shared/types'
import type { Submission, SubmissionLanguage } from '../../shared/types'
import { fullTime } from '../../shared/format'
import { useMediaQuery } from '../../shared/useMediaQuery'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import styles from './ComparePage.module.css'

const CodeDiffEditor = lazy(() => import('./CodeDiffEditor'))

/**
 * 코드 비교 `/submissions/compare?a=&b=` (디자인 설계서 §9.3).
 *
 * 같은 문제의 내 제출 두 개만 견준다. 왼쪽이 먼저 낸 것이다. 그룹별 판정이 어떻게 바뀌었는지를
 * 코드 차이와 함께 보여, "무엇을 고쳤더니 무엇이 풀렸나"에 답한다.
 *
 * 시간·메모리는 직접 견주지 않는다 — 같은 런타임·같은 테스트 버전이라는 보장이 화면에 없다 (§9.3).
 */
export function ComparePage() {
  const params = new URLSearchParams(window.location.search)
  const ids = [params.get('a'), params.get('b')].filter((id): id is string => !!id)
  const index = useProblemIndex()
  const wide = useMediaQuery('(min-width: 900px)')

  const results = useQueries({
    queries: ids.flatMap((id) => [
      { queryKey: ['submission', id], queryFn: () => getSubmission(id) },
      { queryKey: ['submission', id, 'source'], queryFn: () => getSubmissionSource(id) },
    ]),
  })
  const [a, aSource, b, bSource] = results.map((result) => result.data) as [
    Submission | undefined,
    string | null | undefined,
    Submission | undefined,
    string | null | undefined,
  ]

  if (ids.length !== 2 || results.some((result) => result.isError)) {
    return (
      <Shell>
        <EmptyState
          title="비교할 두 제출을 찾지 못했습니다"
          action={
            <Link href="/submissions" className="linklike">
              내 제출에서 고르기
            </Link>
          }
        >
          내 제출 목록에서 같은 문제의 제출 두 개를 고르세요.
        </EmptyState>
      </Shell>
    )
  }

  if (!a || !b || aSource === undefined || bSource === undefined) {
    return (
      <Shell>
        <div role="status" aria-busy="true" className={styles.loading}>
          <span className="visually-hidden">불러오는 중</span>
          <Skeleton height={60} />
          <Skeleton height={320} />
        </div>
      </Shell>
    )
  }

  if (a.problemId !== b.problemId) {
    return (
      <Shell>
        <EmptyState title="다른 문제의 제출은 견주지 않습니다">
          같은 문제의 제출 두 개만 비교할 수 있습니다 — 다른 문제의 코드는 차이가 뜻을 갖지 않습니다.
        </EmptyState>
      </Shell>
    )
  }

  const language = (b.language as SubmissionLanguage) ?? 'KOTLIN'

  return (
    <Shell>
      <h1 className={styles.title}>{problemLabel(index, a.problemId)} — 두 제출 비교</h1>
      <div className={styles.sides}>
        <Side label="먼저" submission={a} />
        <Side label="나중" submission={b} />
      </div>

      <GroupChanges before={a} after={b} />

      {aSource === null || bSource === null ? (
        <p className={styles.muted}>한쪽 제출의 코드가 남아 있지 않아 코드를 견줄 수 없습니다.</p>
      ) : (
        <div className={styles.diff}>
          <Suspense fallback={<Skeleton height={420} />}>
            <CodeDiffEditor
              original={aSource}
              modified={bSource}
              language={EDITOR_LANGUAGE[language] ?? 'plaintext'}
              sideBySide={wide}
            />
          </Suspense>
        </div>
      )}
    </Shell>
  )
}

function Shell({ children }: { children: ReactNode }) {
  return (
    <div className={styles.page}>
      <Link href="/submissions" className={styles.back}>
        <ArrowLeft size={16} aria-hidden="true" />
        내 제출
      </Link>
      {children}
    </div>
  )
}

function Side({ label, submission }: { label: string; submission: Submission }) {
  return (
    <Link href={`/submissions/${submission.id}`} className={styles.side}>
      <span className={styles.sideLabel}>{label}</span>
      {IN_FLIGHT.has(submission.status) ? (
        <JudgeStatusBadge status={submission.status} />
      ) : (
        submission.verdict && <VerdictBadge verdict={submission.verdict} />
      )}
      <span className={styles.score}>{submission.score ?? '—'}점</span>
      <span className={styles.muted}>
        {LANGUAGE_LABEL[submission.language as SubmissionLanguage] ?? submission.language}
        {submission.createdAt && ` · ${fullTime(submission.createdAt)}`}
      </span>
    </Link>
  )
}

/** 그룹별 판정이 바뀐 것만 (디자인 설계서 §9.3 "변경된 코드와 테스트 그룹 변화") */
function GroupChanges({ before, after }: { before: Submission; after: Submission }) {
  const changes = (after.groups ?? [])
    .map((group) => ({ group, prior: before.groups?.find((g) => g.groupId === group.groupId) }))
    .filter(({ group, prior }) => prior && prior.verdict !== group.verdict)
  if (changes.length === 0) return null
  return (
    <ul className={styles.changes} aria-label="그룹 판정 변화">
      {changes.map(({ group, prior }) => (
        <li key={group.groupId}>
          <code>{group.groupId}</code>
          <VerdictBadge verdict={prior!.verdict} />
          <span aria-hidden="true">→</span>
          <span className="visually-hidden">에서</span>
          <VerdictBadge verdict={group.verdict} />
        </li>
      ))}
    </ul>
  )
}
