/**
 * 로고 — 판정 괄호 (docs/ui-overhaul.md §5.3).
 *
 * 괄호는 글자색을, 체크는 브랜드색을 따른다. 그래서 테마가 바뀌어도 그림 한 벌로 된다.
 * 파비콘(`public/favicon.svg`)은 같은 경로를 고정색 타일 위에 그린 것이다 — 여기를 고치면 그쪽도 고친다.
 */
export function LogoMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 120 120" aria-hidden="true" focusable="false">
      <path d="M40 16H22v88h18M80 16h18v88H80" fill="none" stroke="currentColor" strokeWidth="13" strokeLinecap="square" />
      <path
        d="M42 62l13 13 24-30"
        fill="none"
        stroke="var(--color-brand)"
        strokeWidth="12"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
