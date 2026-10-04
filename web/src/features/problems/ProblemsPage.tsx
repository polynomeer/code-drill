import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ArrowDown, ArrowUp, ArrowUpDown, Check, Circle, Shuffle, Tags } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useLocation } from 'wouter'
import { listProblems, randomProblem } from '../../api/client'
import { useSession } from '../../api/session'
import {
  Button,
  Chip,
  DifficultyBadge,
  EmptyState,
  InlineAlert,
  Pagination,
  SearchField,
  Skeleton,
  useToast,
} from '../../design'
import { DIFFICULTIES, DIFFICULTY_LABEL } from '../../shared/types'
import type { ProblemFilter, ProblemSort, ProblemSummary } from '../../shared/types'
import { replaceParams } from '../../shared/url'
import { TodaySummary } from '../training/TodaySummary'
import { clearFilters, isEmpty, readFilter, toggle, writeFilter } from './problemFilter'
import styles from './ProblemsPage.module.css'
import { count } from '../../shared/format'
import { markOpenSource, trackOnce } from '../../shared/analytics'

/**
 * P-01 문제 탐색 `/problems` (UI 디자인 문서 §3, docs/ui-overhaul.md §6.1).
 *
 * "문제 수가 많아 보이는 곳"이 아니라 "지금 풀 문제를 빠르게 결정하는 곳"이다. 검색과 필터는
 * 표 바로 위, 학습 맥락은 보조 열에만 둔다.
 *
 * 로그인하지 않아도 열린다 (디자인 설계서 §11.1). 로그인했으면 완료 상태 열과 상태 필터가 더 생긴다.
 *
 * 필터·정렬·쪽은 주소에 산다 (FR-201). 새로고침해도 같고 링크로 건넬 수 있다.
 */
const PAGE_SIZE = 50
const TOP_TAGS = 8

/** 이 화면이 주소에서 소유하는 키. 여기 없는 키는 건드리지 않는다. */
const FILTER_KEYS = ['query', 'difficulty', 'tags', 'status', 'sort', 'order', 'page']

export function ProblemsPage() {
  const session = useSession()
  const [, navigate] = useLocation()
  const toast = useToast()

  const [filter, setFilter] = useState<ProblemFilter>(() => readFilter(window.location.search))
  // 검색어는 타이핑마다 요청하지 않는다. 잠잠해지면 한 번 보낸다 (UI 디자인 문서 §3.3 — 250ms).
  const [typed, setTyped] = useState(filter.query)
  useEffect(() => {
    if (typed === filter.query) return
    const timer = setTimeout(() => setFilter((prev) => ({ ...prev, query: typed, page: 1 })), 250)
    return () => clearTimeout(timer)
  }, [typed, filter.query])

  useEffect(() => {
    replaceParams(new URLSearchParams(writeFilter(filter)), FILTER_KEYS)
  }, [filter])

  // 새 조건의 결과가 올 때까지 앞 결과를 둔다 — 비웠다가 다시 채우면 표가 깜빡이며 자리를 잃는다.
  const query = useQuery({
    queryKey: ['problems', 'page', filter, session?.userId ?? null],
    queryFn: () => listProblems(filter, PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const page = query.data
  const loading = query.isFetching || typed !== filter.query

  // 목록을 봤다 — 같은 조건은 이 탭에서 한 번 (§16.1 problem_list_view, 탐색 마찰)
  useEffect(() => {
    if (!page || query.isPlaceholderData) return
    const filters = [filter.query.trim() !== '', filter.difficulty.length > 0, filter.tags.length > 0, filter.status !== null].filter(Boolean).length
    trackOnce(`list:${JSON.stringify(filter)}`, 'problem_list_view', { filters, resultCount: page.total ?? 0, sort: filter.sort })
  }, [page, query.isPlaceholderData, filter])

  /** 거르는 조건이 바뀌면 1쪽으로 돌아간다. 3쪽을 보다 태그를 바꾸면 3쪽이 없을 수 있다. */
  const update = (patch: Partial<ProblemFilter>) => setFilter((prev) => ({ ...prev, ...patch, page: 1 }))

  const sortBy = (sort: ProblemSort) =>
    setFilter((prev) => ({
      ...prev,
      sort,
      // 같은 열을 다시 누르면 방향을 뒤집는다. 다른 열이면 그 열의 자연스러운 첫 방향으로.
      order: prev.sort === sort ? (prev.order === 'ASC' ? 'DESC' : 'ASC') : sort === 'SOLVERS' ? 'DESC' : 'ASC',
      page: 1,
    }))

  const reset = () => {
    setTyped('')
    setFilter((prev) => clearFilters(prev))
  }

  const pick = async () => {
    try {
      const picked = await randomProblem(filter)
      if (picked) {
        markOpenSource('list')
        navigate(problemHref(picked.id, session !== null))
      }
      else toast.show('조건에 맞는 문제가 없습니다', 'warning')
    } catch {
      toast.show('문제를 고르지 못했습니다', 'danger')
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.main}>
        <header className={styles.head}>
          <div>
            <h1 className={styles.title}>문제</h1>
            <p className={styles.count} role="status">
              {page ? `${count(page.total)}문제` : ' '}
              {!isEmpty(filter) && page && ' · 조건에 맞는 것'}
            </p>
          </div>
          <Button icon={<Shuffle size={16} />} onClick={() => void pick()}>
            아무 문제나
          </Button>
        </header>

        <section className={styles.filters} aria-label="검색과 필터">
          <SearchField
            label="문제 검색"
            placeholder="제목이나 번호로 검색 (예: 1042)"
            value={typed}
            onChange={(event) => setTyped(event.target.value)}
          />
          <div className={styles.filterRow}>
            <FilterGroup label="난이도">
              {DIFFICULTIES.map((level) => (
                <Chip
                  key={level}
                  pressed={filter.difficulty.includes(level)}
                  onClick={() => update({ difficulty: toggle(filter.difficulty, level) })}
                >
                  {DIFFICULTY_LABEL[level]}
                </Chip>
              ))}
            </FilterGroup>
            {session && (
              <FilterGroup label="상태">
                {(['UNSOLVED', 'SOLVED'] as const).map((value) => (
                  <Chip
                    key={value}
                    pressed={filter.status === value}
                    onClick={() => update({ status: filter.status === value ? null : value })}
                  >
                    {value === 'SOLVED' ? '푼 문제' : '안 푼 문제'}
                  </Chip>
                ))}
              </FilterGroup>
            )}
          </div>
          <TagFilter
            counts={page?.tags ?? {}}
            selected={filter.tags}
            onToggle={(tag) => update({ tags: toggle(filter.tags, tag) })}
          />
          {!isEmpty(filter) && (
            <button type="button" className={`linklike ${styles.reset}`} onClick={reset}>
              필터 지우기
            </button>
          )}
        </section>

        {query.isError && (
          <InlineAlert
            tone="danger"
            title="문제 목록을 불러오지 못했습니다"
            action={
              <Button size="dense" onClick={() => void query.refetch()}>
                다시 시도
              </Button>
            }
          />
        )}

        {!page && !query.isError ? (
          <TableSkeleton />
        ) : page && page.total === 0 ? (
          <div className={styles.empty}>
            <EmptyState title="조건에 맞는 문제가 없습니다" action={<Button onClick={reset}>필터 초기화</Button>}>
              {describeFilter(filter)}
            </EmptyState>
          </div>
        ) : (
          page && (
            <ProblemTable
              items={page.items}
              signedIn={session !== null}
              filter={filter}
              onSort={sortBy}
              busy={loading}
            />
          )
        )}

        {page?.page && page.pageCount ? (
          <Pagination
            page={page.page}
            pageCount={page.pageCount}
            onChange={(next) => {
              setFilter((prev) => ({ ...prev, page: next }))
              window.scrollTo({ top: 0 })
            }}
          />
        ) : null}
      </div>

      {/* 보조 열 (UI 디자인 문서 §3.2 — 250~300px). 좁으면 표 아래로 내려간다. */}
      <aside className={styles.side} aria-label="학습 맥락">
        {session ? (
          <TodaySummary />
        ) : (
          <section className={styles.cta}>
            <h2>풀이를 기록하세요</h2>
            <p>
              가입하면 푼 문제와 오답의 분기점, 약한 역량을 근거와 함께 쌓습니다. 둘러보기와 문제 읽기는
              로그인 없이 됩니다.
            </p>
            <Link href={`/login?next=${encodeURIComponent('/problems')}`} className={styles.ctaLink}>
              로그인 · 가입
            </Link>
          </section>
        )}
      </aside>
    </div>
  )
}

/* ─── 표 ─── */

const COLUMNS: { sort: ProblemSort; label: string; className?: string }[] = [
  { sort: 'NUMBER', label: '번호', className: styles.colNumber },
  { sort: 'TITLE', label: '제목' },
  { sort: 'DIFFICULTY', label: '난이도', className: styles.colDifficulty },
  { sort: 'ACCURACY', label: '정답률', className: styles.colRate },
  { sort: 'SOLVERS', label: '맞힌 사람', className: styles.colSolvers },
]

function ProblemTable({
  items,
  signedIn,
  filter,
  onSort,
  busy,
}: {
  items: ProblemSummary[]
  signedIn: boolean
  filter: ProblemFilter
  onSort: (sort: ProblemSort) => void
  busy: boolean
}) {
  return (
    <table className={styles.table} aria-busy={busy}>
      <caption className="visually-hidden">문제 목록. 열 제목을 누르면 그 열로 정렬합니다.</caption>
      <thead>
        <tr>
          {signedIn && (
            <th scope="col" className={styles.colStatus}>
              <span className="visually-hidden">상태</span>
            </th>
          )}
          {COLUMNS.map((column) => {
            const active = filter.sort === column.sort
            const Icon = !active ? ArrowUpDown : filter.order === 'ASC' ? ArrowUp : ArrowDown
            return (
              <th
                key={column.sort}
                scope="col"
                className={column.className}
                aria-sort={active ? (filter.order === 'ASC' ? 'ascending' : 'descending') : undefined}
              >
                <button type="button" className={styles.sortButton} onClick={() => onSort(column.sort)}>
                  {column.label}
                  <Icon size={14} aria-hidden="true" className={active ? styles.sortActive : styles.sortIdle} />
                </button>
              </th>
            )
          })}
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.id}>
            {signedIn && (
              <td className={styles.colStatus}>
                {/* 색만으로 구분하지 않는다 — 아이콘 모양과 숨은 글이 함께 간다 (UI 디자인 문서 §3.4) */}
                {item.solved ? (
                  <Check size={16} className={styles.solved} aria-label="푼 문제" />
                ) : (
                  <Circle size={10} className={styles.unsolved} aria-label="아직 안 푼 문제" />
                )}
              </td>
            )}
            <td className={styles.colNumber}>{item.number ?? '—'}</td>
            <td className={styles.colTitle}>
              {/* 행 전체가 링크다. 진짜 <a> 라서 새 탭으로도 열린다 */}
              <Link href={problemHref(item.id, signedIn)} className={styles.rowLink} onClick={() => markOpenSource('list')}>
                {item.title}
              </Link>
              <span className={styles.tags}>{item.tags.join(' · ')}</span>
            </td>
            <td className={styles.colDifficulty}>
              <DifficultyBadge level={item.difficulty} />
            </td>
            <td className={styles.colRate}>
              {/* 표본이 적으면 서버가 null 을 준다. 없는 값을 0% 로 그리지 않는다. */}
              {item.solvedRate === null ? (
                <span className={styles.muted} title="풀어 본 사람이 아직 적습니다">
                  —
                </span>
              ) : (
                `${(item.solvedRate * 100).toFixed(1)}%`
              )}
            </td>
            <td className={styles.colSolvers}>{count(item.solvedCount)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function TableSkeleton() {
  return (
    <div className={styles.skeleton} role="status" aria-busy="true">
      <span className="visually-hidden">문제 목록을 불러오는 중</span>
      {Array.from({ length: 8 }, (_, index) => (
        <div key={index} className={styles.skeletonRow}>
          <Skeleton width={40} />
          <Skeleton width={`${40 + ((index * 17) % 35)}%`} />
          <Skeleton width={60} />
        </div>
      ))}
    </div>
  )
}

/* ─── 태그 ─── */

/**
 * 태그는 접는다. 펼쳐 두면 칩 서른다섯 개가 표보다 먼저 400px 를 차지했다 (ux-benchmark.md §6.1).
 * 많이 쓰이는 여덟 개와 고른 것만 보이고, 나머지는 "태그 더 보기"에서 찾는다.
 */
function TagFilter({
  counts,
  selected,
  onToggle,
}: {
  counts: Record<string, number>
  selected: string[]
  onToggle: (tag: string) => void
}) {
  const [search, setSearch] = useState('')
  const popover = useRef<HTMLDivElement>(null)
  const searchInput = useRef<HTMLInputElement>(null)
  // 팝오버가 열리면 바로 찾기 칸에 쓴다. React 의 autoFocus 는 그리는 순간(닫혀 있을 때) 한 번만
  // 포커스를 주므로 여는 순간을 따로 듣는다.
  useEffect(() => {
    const element = popover.current
    if (!element) return
    const onToggle = (event: Event) => {
      if ((event as ToggleEvent).newState === 'open') searchInput.current?.focus()
    }
    element.addEventListener('toggle', onToggle)
    return () => element.removeEventListener('toggle', onToggle)
  })
  // 고른 태그는 결과가 0이어도 남긴다 — 칩이 사라지면 걸린 필터를 끌 버튼이 없어진다.
  const all = useMemo(
    () =>
      [...new Set([...Object.keys(counts), ...selected])]
        .map((tag) => [tag, counts[tag] ?? 0] as const)
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])),
    [counts, selected],
  )
  const top = all.filter(([tag], index) => index < TOP_TAGS || selected.includes(tag))
  const found = all.filter(([tag]) => tag.includes(search.trim().toLowerCase()))

  if (all.length === 0) return null
  return (
    <FilterGroup label="태그">
      {top.map(([tag, count]) => (
        <Chip key={tag} pressed={selected.includes(tag)} count={count} onClick={() => onToggle(tag)}>
          {tag}
        </Chip>
      ))}
      {all.length > top.length && (
        <>
          <button type="button" className={styles.moreTags} popoverTarget="tag-popover">
            <Tags size={14} aria-hidden="true" />
            태그 더 보기 ({all.length - top.length})
          </button>
          <div id="tag-popover" popover="auto" className={styles.tagPopover} ref={popover}>
            <SearchField
              label="태그 찾기"
              placeholder="태그 찾기"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              ref={searchInput}
            />
            <div className={styles.tagGrid}>
              {found.map(([tag, count]) => (
                <Chip key={tag} pressed={selected.includes(tag)} count={count} onClick={() => onToggle(tag)}>
                  {tag}
                </Chip>
              ))}
              {found.length === 0 && <p className={styles.muted}>찾는 태그가 없습니다</p>}
            </div>
          </div>
        </>
      )}
    </FilterGroup>
  )
}

function FilterGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <fieldset className={styles.group}>
      <legend className={styles.groupLabel}>{label}</legend>
      <div className={styles.groupBody}>{children}</div>
    </fieldset>
  )
}

/* ─── 도움 ─── */

/** 로그인했으면 바로 풀이로, 아니면 읽기 화면으로. 읽기 화면에서 로그인하고 풀이로 간다. */
export function problemHref(slug: string, signedIn: boolean): string {
  return signedIn ? `/problems/${slug}/solve` : `/problems/${slug}`
}

/** 빈 결과에서 무엇이 걸려 있는지 보여 준다 (UI 디자인 문서 §3.3) */
function describeFilter(filter: ProblemFilter): string {
  const parts = [
    filter.query.trim() && `검색: ${filter.query.trim()}`,
    filter.difficulty.length > 0 && `난이도: ${filter.difficulty.map((d) => DIFFICULTY_LABEL[d]).join(', ')}`,
    filter.tags.length > 0 && `태그: ${filter.tags.join(', ')}`,
    filter.status && (filter.status === 'SOLVED' ? '푼 문제만' : '안 푼 문제만'),
  ].filter(Boolean)
  return parts.join(' · ')
}
