import { useInfiniteQuery } from '@tanstack/react-query'
import { GitCompareArrows, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation } from 'wouter'
import { listSubmissions } from '../../api/client'
import { Badge, Button, EmptyState, InlineAlert, JudgeStatusBadge, Select, Skeleton, VerdictBadge } from '../../design'
import { IN_FLIGHT, LANGUAGE_LABEL, VERDICT_LABEL } from '../../shared/types'
import type { Submission, SubmissionLanguage, Verdict } from '../../shared/types'
import { fullTime, relativeTime } from '../../shared/time'
import { replaceParams } from '../../shared/url'
import { problemLabel, useProblemIndex } from '../problems/useProblemIndex'
import styles from './SubmissionsPage.module.css'

/**
 * H-01 내 제출 `/submissions` (디자인 설계서 §9.1 — docs/ui-overhaul.md §6.3).
 *
 * 최신순 고정이다 (서버의 커서 순서). 판정·언어·문제로 거르고, 같은 문제의 두 제출을 골라 비교로 간다.
 * 거르는 조건은 주소에 산다 — "그 문제에서 틀린 것만"을 링크로 건넬 수 있다.
 */
const VERDICTS = Object.keys(VERDICT_LABEL) as Verdict[]
const LANGUAGES = Object.keys(LANGUAGE_LABEL) as SubmissionLanguage[]

type Filter = { problem: string | null; verdict: Verdict | null; language: SubmissionLanguage | null }

function readFilter(): Filter {
  const params = new URLSearchParams(window.location.search)
  const verdict = params.get('verdict')
  const language = params.get('language')
  return {
    problem: params.get('problem'),
    verdict: VERDICTS.includes(verdict as Verdict) ? (verdict as Verdict) : null,
    language: LANGUAGES.includes(language as SubmissionLanguage) ? (language as SubmissionLanguage) : null,
  }
}

export function SubmissionsPage() {
  const [, navigate] = useLocation()
  const [filter, setFilter] = useState<Filter>(readFilter)
  const [picked, setPicked] = useState<Submission[]>([])
  const index = useProblemIndex()

  useEffect(() => {
    const params = new URLSearchParams()
    if (filter.problem) params.set('problem', filter.problem)
    if (filter.verdict) params.set('verdict', filter.verdict)
    if (filter.language) params.set('language', filter.language)
    replaceParams(params, ['problem', 'verdict', 'language'])
    setPicked([])
  }, [filter])

  const query = useInfiniteQuery({
    queryKey: ['submissions', 'all', filter],
    queryFn: ({ pageParam }) =>
      listSubmissions({
        problemId: filter.problem,
        verdict: filter.verdict,
        language: filter.language,
        cursor: pageParam,
        limit: 30,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
  const items = query.data?.pages.flatMap((page) => page.items) ?? []

  /** 비교는 같은 문제의 두 제출만 (디자인 설계서 §9.3). 셋째를 고르면 가장 먼저 고른 것이 빠진다. */
  const toggle = (item: Submission) =>
    setPicked((prev) => {
      if (prev.some((p) => p.id === item.id)) return prev.filter((p) => p.id !== item.id)
      const same = prev.filter((p) => p.problemId === item.problemId)
      return [...same, item].slice(-2)
    })

  const compare = () => {
    const [a, b] = [...picked].sort((x, y) => (x.createdAt ?? '').localeCompare(y.createdAt ?? ''))
    if (a && b) navigate(`/submissions/compare?a=${a.id}&b=${b.id}`)
  }

  const filtered = filter.problem !== null || filter.verdict !== null || filter.language !== null

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <h1 className={styles.title}>내 제출</h1>
        <Button
          icon={<GitCompareArrows size={16} />}
          disabled={picked.length !== 2}
          onClick={compare}
          title="같은 문제의 제출 두 개를 고르면 코드와 판정을 나란히 봅니다"
        >
          두 제출 비교 {picked.length > 0 && `(${picked.length}/2)`}
        </Button>
      </header>

      <div className={styles.filters}>
        {filter.problem && (
          <Badge tone="brand">
            {problemLabel(index, filter.problem)}
            <button
              type="button"
              className={styles.clear}
              aria-label="문제 조건 지우기"
              onClick={() => setFilter((prev) => ({ ...prev, problem: null }))}
            >
              <X size={12} aria-hidden="true" />
            </button>
          </Badge>
        )}
        <Select
          label="판정"
          value={filter.verdict ?? ''}
          onChange={(event) => setFilter((prev) => ({ ...prev, verdict: (event.target.value || null) as Verdict | null }))}
        >
          <option value="">모든 판정</option>
          {VERDICTS.map((verdict) => (
            <option key={verdict} value={verdict}>
              {VERDICT_LABEL[verdict]}
            </option>
          ))}
        </Select>
        <Select
          label="언어"
          value={filter.language ?? ''}
          onChange={(event) =>
            setFilter((prev) => ({ ...prev, language: (event.target.value || null) as SubmissionLanguage | null }))
          }
        >
          <option value="">모든 언어</option>
          {LANGUAGES.map((language) => (
            <option key={language} value={language}>
              {LANGUAGE_LABEL[language]}
            </option>
          ))}
        </Select>
      </div>

      {query.isError && (
        <InlineAlert
          tone="danger"
          title="제출 기록을 불러오지 못했습니다"
          action={
            <Button size="dense" onClick={() => void query.refetch()}>
              다시 시도
            </Button>
          }
        />
      )}

      {query.isPending ? (
        <div className={styles.loading} role="status" aria-busy="true">
          <span className="visually-hidden">불러오는 중</span>
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} height={20} />
          ))}
        </div>
      ) : items.length === 0 && !query.isError ? (
        <div className={styles.empty}>
          <EmptyState
            title={filtered ? '조건에 맞는 제출이 없습니다' : '아직 제출이 없습니다'}
            action={
              filtered ? (
                <Button onClick={() => setFilter({ problem: null, verdict: null, language: null })}>조건 지우기</Button>
              ) : (
                <Link href="/problems" className="linklike">
                  문제 고르러 가기
                </Link>
              )
            }
          />
        </div>
      ) : (
        <table className={styles.table}>
          <caption className="visually-hidden">내 제출, 최신순. 같은 문제의 두 제출을 골라 비교할 수 있습니다.</caption>
          <thead>
            <tr>
              <th scope="col" className={styles.colPick}>
                <span className="visually-hidden">비교할 제출 고르기</span>
              </th>
              <th scope="col">제출 시각</th>
              <th scope="col">문제</th>
              <th scope="col">판정</th>
              <th scope="col" className={styles.colScore}>
                점수
              </th>
              <th scope="col" className={styles.colLanguage}>
                언어
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => {
              const chosen = picked.some((p) => p.id === item.id)
              return (
                <tr key={item.id} className={chosen ? styles.chosen : undefined}>
                  <td className={styles.colPick}>
                    <input
                      type="checkbox"
                      checked={chosen}
                      onChange={() => toggle(item)}
                      aria-label={`${problemLabel(index, item.problemId)} 제출 비교에 넣기`}
                    />
                  </td>
                  <td className={styles.colTime}>
                    {item.createdAt ? (
                      <time dateTime={item.createdAt} title={fullTime(item.createdAt)}>
                        {relativeTime(item.createdAt)}
                      </time>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    <Link href={`/submissions/${item.id}`} className={styles.rowLink}>
                      {problemLabel(index, item.problemId)}
                    </Link>
                  </td>
                  <td>
                    {IN_FLIGHT.has(item.status) ? (
                      <JudgeStatusBadge status={item.status} />
                    ) : item.verdict ? (
                      <VerdictBadge verdict={item.verdict} />
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className={styles.colScore}>{item.score ?? '—'}</td>
                  <td className={styles.colLanguage}>
                    {LANGUAGE_LABEL[item.language as SubmissionLanguage] ?? item.language}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}

      {query.hasNextPage && (
        <div className={styles.more}>
          <Button loading={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()}>
            더 보기
          </Button>
        </div>
      )}
    </div>
  )
}
