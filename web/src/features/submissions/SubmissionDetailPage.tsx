import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Clapperboard, Code, PenLine } from 'lucide-react'
import { Suspense, lazy } from 'react'
import { Link, Redirect, useLocation } from 'wouter'
import { getProblem, getSubmission, getSubmissionSource, getTraceManifest } from '../../api/client'
import { EmptyState, Skeleton } from '../../design'
import { EDITOR_LANGUAGE, LANGUAGE_LABEL } from '../../shared/types'
import type { SubmissionLanguage } from '../../shared/types'
import { fullTime } from '../../shared/time'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import { VerdictPanel } from './VerdictPanel'
import styles from './SubmissionDetailPage.module.css'

const MonacoWorkspace = lazy(() => import('../workspace/MonacoWorkspace'))

/**
 * 제출 상세 `/submissions/:id` (디자인 설계서 §9.2).
 *
 * 위에 판정·시각·언어·문제 버전과 "다시 풀기", 그 아래 판정, 그리고 읽기 전용 코드. 코드는
 * 고칠 수 없다 — "이 코드로 편집기 열기"가 그 코드를 새 초안으로 풀이 화면에 옮긴다.
 *
 * 리플레이 전용 화면(R-01)은 U4 에서 선다. 그 전까지 리플레이는 풀이 화면의 결과 창에서 연다.
 */
export function SubmissionDetailPage({ id }: { id: string }) {
  const [, navigate] = useLocation()
  const index = useProblemIndex()
  const submissionQuery = useQuery({ queryKey: ['submission', id], queryFn: () => getSubmission(id) })
  const submission = submissionQuery.data
  const problemQuery = useQuery({
    queryKey: ['problem', submission?.problemId],
    queryFn: () => getProblem(submission!.problemId),
    enabled: !!submission,
  })
  const mine = submission?.mine !== false
  const sourceQuery = useQuery({
    queryKey: ['submission', id, 'source'],
    queryFn: () => getSubmissionSource(id),
    enabled: !!submission && mine,
  })
  const traceQuery = useQuery({
    queryKey: ['submission', id, 'trace'],
    queryFn: () => getTraceManifest(id),
    enabled: !!submission,
  })

  if (submissionQuery.isError) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="제출을 찾지 못했습니다"
          action={
            <Link href="/submissions" className="linklike">
              내 제출로
            </Link>
          }
        >
          지웠거나 볼 수 없는 제출입니다.
        </EmptyState>
      </div>
    )
  }

  if (!submission) {
    return (
      <div className={styles.page} role="status" aria-busy="true">
        <span className="visually-hidden">불러오는 중</span>
        <Skeleton width="40%" height={28} />
        <Skeleton height={120} />
      </div>
    )
  }

  const language = submission.language as SubmissionLanguage
  const solve = `/problems/${submission.problemId}/solve`
  // `?step=` 는 리플레이의 한 걸음을 가리키는 링크다 (게시판 붙임, 예전 주소). 그 걸음으로 바로 간다.
  const step = new URLSearchParams(window.location.search).get('step')
  const replay = `${solve}?submission=${submission.id}&step=${step ?? 0}`
  if (step !== null) return <Redirect to={replay} replace />
  const trace = traceQuery.data ?? null

  return (
    <div className={styles.page}>
      <Link href="/submissions" className={styles.back}>
        <ArrowLeft size={16} aria-hidden="true" />
        내 제출
      </Link>

      <header className={styles.head}>
        <div>
          <h1 className={styles.title}>
            <Link href={`/problems/${submission.problemId}`}>{problemLabel(index, submission.problemId)}</Link>
          </h1>
          <dl className={styles.meta}>
            {submission.createdAt && (
              <div>
                <dt>제출</dt>
                <dd>
                  <time dateTime={submission.createdAt}>{fullTime(submission.createdAt)}</time>
                </dd>
              </div>
            )}
            <div>
              <dt>언어</dt>
              <dd>{LANGUAGE_LABEL[language] ?? submission.language}</dd>
            </div>
            <div>
              <dt>문제 버전</dt>
              <dd>v{submission.problemVersion}</dd>
            </div>
          </dl>
        </div>
        <div className={styles.actions}>
          {trace && trace.status !== 'EMPTY' && (
            <Link href={replay} className={styles.secondary}>
              <Clapperboard size={16} aria-hidden="true" />
              리플레이
            </Link>
          )}
          {mine && sourceQuery.data && (
            <Link href={`${solve}?lang=${language}&from=${submission.id}`} className={styles.secondary}>
              <PenLine size={16} aria-hidden="true" />이 코드로 편집기 열기
            </Link>
          )}
          <Link href={solve} className={styles.primary}>
            <Code size={16} aria-hidden="true" />
            다시 풀기
          </Link>
        </div>
      </header>

      <section className={styles.card} aria-label="판정">
        <VerdictPanel
          submission={submission}
          problem={problemQuery.data ?? null}
          hasTrace={trace !== null && trace.status !== 'EMPTY'}
          onOpenReplay={() => navigate(replay)}
          onOpenEditorial={() => navigate(solve)}
        />
      </section>

      {mine && (
        <section className={styles.card} aria-labelledby="code-title">
          <h2 id="code-title" className={styles.cardTitle}>
            제출한 코드
          </h2>
          {sourceQuery.isPending ? (
            <Skeleton height={200} />
          ) : sourceQuery.data == null ? (
            // 계정을 지운 사용자의 소스는 비어 있다 (§11.3) — 없는 것과 지운 것을 가른다.
            <p className={styles.muted}>이 제출의 코드는 남아 있지 않습니다.</p>
          ) : (
            <div className={styles.code} style={{ height: codeHeight(sourceQuery.data) }}>
              <Suspense fallback={<Skeleton height={200} />}>
                <MonacoWorkspace
                  source={sourceQuery.data}
                  language={EDITOR_LANGUAGE[language] ?? 'plaintext'}
                  onChange={() => undefined}
                  readOnly
                  label="제출한 코드 (읽기 전용)"
                />
              </Suspense>
            </div>
          )}
        </section>
      )}
    </div>
  )
}

/** 코드 길이만큼 — 너무 짧으면 답답하고 너무 길면 페이지가 끝없이 길어진다 */
function codeHeight(source: string): number {
  const lines = source.split('\n').length
  return Math.min(640, Math.max(160, lines * 20 + 24))
}
