import { useEffect, useState } from 'react'
import { listProblems } from '../../api/client'
import type { ProblemSummary } from '../../shared/types'

/**
 * 문제 탐색 (디자인 설계서 §2.1).
 *
 * 검색은 서버가 한다. 목록 전체를 받아 클라이언트에서 거르면, 문제가 늘어난 뒤에
 * 커서 페이지네이션과 어긋난다.
 */
export function ProblemList({
  selected,
  onSelect,
}: {
  selected: string | null
  onSelect: (slug: string) => void
}) {
  const [query, setQuery] = useState('')
  const [items, setItems] = useState<ProblemSummary[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    // 타이핑마다 요청하지 않는다. 잠잠해지면 한 번 보낸다.
    const timer = setTimeout(() => {
      listProblems(query)
        .then((page) => setItems(page.items))
        .finally(() => setLoading(false))
    }, 250)
    return () => clearTimeout(timer)
  }, [query])

  return (
    <section className="panel">
      <h3>문제</h3>
      <input
        className="search"
        type="search"
        value={query}
        placeholder="제목으로 검색"
        onChange={(event) => setQuery(event.target.value)}
        aria-label="문제 검색"
      />
      {items.length === 0 && !loading && <p className="muted">검색 결과가 없습니다.</p>}
      <ul className="problem-list">
        {items.map((item) => (
          <li key={item.id} className={item.id === selected ? 'current' : ''}>
            <button onClick={() => onSelect(item.id)}>
              <span>{item.title}</span>
              <span className="muted mono">{item.id}</span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
