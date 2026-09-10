import { useEffect, useState } from 'react'
import { listProblems } from '../../api/client'
import {
  DIFFICULTIES,
  DIFFICULTY_LABEL,
  EMPTY_FILTER,
  type ProblemFilter,
  type ProblemPage,
} from '../../shared/types'
import { replaceParams } from '../../shared/url'
import { isEmpty, readFilter, toggle, writeFilter } from './problemFilter'

/** 이 화면이 주소에서 소유하는 키. 여기 없는 키는 건드리지 않는다. */
const FILTER_KEYS = ['query', 'difficulty', 'tags', 'status']

const EMPTY_PAGE: ProblemPage = { items: [], nextCursor: null, total: 0, tags: {} }

/**
 * 문제 탐색 (디자인 설계서 §2.1, PRD FR-201~203).
 *
 * 필터는 서버가 건다. 목록 전체를 받아 클라이언트에서 거르면, 문제가 늘어난 뒤에
 * 커서 페이지네이션과 어긋난다.
 *
 * 필터 상태는 URL 에 산다 (problemFilter.ts). 새로고침해도 같고, 링크로 건넬 수 있다.
 */
export function ProblemList({
  selected,
  onSelect,
}: {
  selected: string | null
  onSelect: (slug: string) => void
}) {
  const [filter, setFilter] = useState<ProblemFilter>(() => readFilter(window.location.search))
  const [page, setPage] = useState<ProblemPage>(EMPTY_PAGE)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    // 타이핑마다 요청하지 않는다. 잠잠해지면 한 번 보낸다.
    const timer = setTimeout(() => {
      listProblems(filter)
        .then(setPage)
        .finally(() => setLoading(false))
    }, 250)
    return () => clearTimeout(timer)
  }, [filter])

  // 필터가 바뀌면 주소의 **필터 키만** 갈아 끼운다. 열어 둔 제출(`?submission`)은
  // App 의 것이라 건드리지 않는다.
  useEffect(() => {
    replaceParams(new URLSearchParams(writeFilter(filter)), FILTER_KEYS)
  }, [filter])

  // 태그 후보는 서버가 센다. 화면에 온 항목에서 모으면 **이번 페이지에 없는 태그가
  // 사라져**, 20번째 뒤에만 있는 태그로는 아예 거를 수 없게 된다.
  //
  // 고른 태그는 결과가 0이어도 남긴다. 서버는 결과가 있는 태그만 보내므로, 다른 필터와
  // 겹쳐 0이 되는 순간 그 칩이 사라진다 — 필터는 그대로 걸려 있는데 끄는 버튼만 없어져,
  // 사용자는 "0개"를 보며 왜 그런지도, 어떻게 되돌리는지도 알 수 없다. 실제로 그랬다.
  const tags = [...new Set([...Object.keys(page.tags), ...filter.tags])]
    .sort()
    .map((tag) => [tag, page.tags[tag] ?? 0] as const)

  const update = (patch: Partial<ProblemFilter>) => setFilter((prev) => ({ ...prev, ...patch }))

  return (
    <section className="panel">
      <div className="problem-head">
        <h3>문제</h3>
        {!isEmpty(filter) && (
          <button type="button" className="linklike" onClick={() => setFilter(EMPTY_FILTER)}>
            필터 지우기
          </button>
        )}
      </div>

      <input
        className="search"
        type="search"
        value={filter.query}
        placeholder="제목으로 검색"
        onChange={(event) => update({ query: event.target.value })}
        aria-label="문제 검색"
      />

      <div className="filters">
        <fieldset>
          <legend className="muted">난이도</legend>
          {DIFFICULTIES.map((level) => (
            <button
              key={level}
              type="button"
              className={filter.difficulty.includes(level) ? 'chip on' : 'chip'}
              aria-pressed={filter.difficulty.includes(level)}
              onClick={() => update({ difficulty: toggle(filter.difficulty, level) })}
            >
              {DIFFICULTY_LABEL[level]}
            </button>
          ))}
        </fieldset>

        <fieldset>
          <legend className="muted">상태</legend>
          {(['SOLVED', 'UNSOLVED'] as const).map((value) => (
            <button
              key={value}
              type="button"
              className={filter.status === value ? 'chip on' : 'chip'}
              aria-pressed={filter.status === value}
              onClick={() => update({ status: filter.status === value ? null : value })}
            >
              {value === 'SOLVED' ? '푼 문제' : '안 푼 문제'}
            </button>
          ))}
        </fieldset>

        {tags.length > 0 && (
          <fieldset>
            <legend className="muted">태그</legend>
            {tags.map(([tag, count]) => (
              <button
                key={tag}
                type="button"
                className={filter.tags.includes(tag) ? 'chip on mono' : 'chip mono'}
                aria-pressed={filter.tags.includes(tag)}
                onClick={() => update({ tags: toggle(filter.tags, tag) })}
              >
                {tag} {count}
              </button>
            ))}
          </fieldset>
        )}
      </div>

      {/* 결과 수를 늘 보여준다. 필터를 여러 개 켰을 때 "왜 이것만 나오나"에 먼저 답한다. */}
      <p className="muted result-count">
        {loading ? '찾는 중…' : `${page.total}개`}
      </p>

      {page.total === 0 && !loading && <p className="muted">조건에 맞는 문제가 없습니다.</p>}

      <ul className="problem-list">
        {page.items.map((item) => (
          <li key={item.id} className={item.id === selected ? 'current' : ''}>
            <button onClick={() => onSelect(item.id)}>
              <span className="problem-title">
                {/* 색만으로 구분하지 않는다 (디자인 설계서 접근성). */}
                {item.solved && <span aria-label="푼 문제">✓ </span>}
                {item.title}
              </span>
              <span className="problem-meta muted">
                <span className={`level ${item.difficulty.toLowerCase()}`}>
                  {DIFFICULTY_LABEL[item.difficulty]}
                </span>
                {/* 표본이 적으면 서버가 null 을 준다. 없는 값을 0% 로 그리지 않는다. */}
                {item.solvedRate !== null && <span>정답률 {Math.round(item.solvedRate * 100)}%</span>}
                <span className="mono">{item.tags.join(' · ')}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
