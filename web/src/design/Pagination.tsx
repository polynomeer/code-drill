import { ChevronLeft, ChevronRight } from 'lucide-react'
import styles from './Pagination.module.css'

/**
 * 쪽 이동 (디자인 설계서 §14.1 Table/List — docs/ui-overhaul.md §6.1).
 *
 * 무한 스크롤 대신 쪽이다. 무한 스크롤은 "몇 번째 줄에 있었나"를 잃고, 링크로 건넬 수 없다.
 *
 * 쪽이 많으면 처음·끝과 지금 쪽 양옆만 보이고 사이는 "…"로 접는다. 지금 쪽은 aria-current 로
 * 알린다 — 색만으로 "여기"를 말하지 않는다.
 */
export function Pagination({
  page,
  pageCount,
  onChange,
  label = '쪽 이동',
}: {
  page: number
  pageCount: number
  onChange: (page: number) => void
  label?: string
}) {
  if (pageCount <= 1) return null
  return (
    <nav aria-label={label} className={styles.pagination}>
      <button
        type="button"
        className={styles.step}
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        aria-label="이전 쪽"
      >
        <ChevronLeft size={16} aria-hidden="true" />
      </button>
      {pages(page, pageCount).map((item, index) =>
        item === 'gap' ? (
          <span key={`gap-${index}`} className={styles.gap} aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            className={styles.page}
            aria-current={item === page ? 'page' : undefined}
            aria-label={`${item}쪽`}
            onClick={() => onChange(item)}
          >
            {item}
          </button>
        ),
      )}
      <button
        type="button"
        className={styles.step}
        onClick={() => onChange(page + 1)}
        disabled={page >= pageCount}
        aria-label="다음 쪽"
      >
        <ChevronRight size={16} aria-hidden="true" />
      </button>
    </nav>
  )
}

/** 1 … 4 5 6 … 12 — 처음·끝·지금 양옆. 하나만 접히면 접지 않고 그 숫자를 보인다. */
export function pages(page: number, pageCount: number): (number | 'gap')[] {
  const shown = new Set([1, pageCount, page - 1, page, page + 1].filter((p) => p >= 1 && p <= pageCount))
  const sorted = [...shown].sort((a, b) => a - b)
  const out: (number | 'gap')[] = []
  for (const [index, value] of sorted.entries()) {
    const previous = sorted[index - 1]
    if (previous !== undefined && value - previous === 2) out.push(previous + 1)
    else if (previous !== undefined && value - previous > 2) out.push('gap')
    out.push(value)
  }
  return out
}
