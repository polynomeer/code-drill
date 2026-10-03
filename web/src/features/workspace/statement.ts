/**
 * 지문 원문(마크다운)을 화면 조각으로 나눈다 (docs/ui-overhaul.md §6.2, §7).
 *
 * 렌더링과 나누기를 분리한다. 나누기는 문자열만 다뤄 단위 시험이 가능하고, 렌더링
 * (marked + DOMPurify)은 DOM 이 있어야 해서 화면 쪽에 둔다 (StatementView).
 */

export type StatementParts = {
  /** 문제 본문. 제목(H1)과 리플레이 계측 절을 뺀 나머지 */
  body: string
  /**
   * `## 실행 리플레이 계측` 절의 본문. 문제가 아니라 플랫폼 기능의 설명이라 지문에서 빼고
   * 리플레이 탭의 도움말로 옮긴다. 그래도 문제마다 부를 SDK 예시가 달라 버리지는 않는다.
   */
  instrumentation: string | null
}

const INSTRUMENTATION = /^##\s*실행 리플레이 계측[^\n]*\n/m

export function splitStatement(markdown: string, title: string): StatementParts {
  let text = markdown.replace(/\r\n/g, '\n')

  // 첫 줄의 H1 이 문제 제목과 같으면 뺀다. 화면이 제목을 이미 크게 그린다.
  const h1 = /^#\s+(.+)\n+/.exec(text)
  if (h1 && h1[1]?.trim() === title.trim()) text = text.slice(h1[0].length)

  const start = INSTRUMENTATION.exec(text)
  if (!start) return { body: text.trim(), instrumentation: null }

  const from = start.index
  const rest = text.slice(from + start[0].length)
  // 다음 같은 수준(##) 제목까지가 이 절이다. 없으면 끝까지.
  const next = /^##\s/m.exec(rest)
  const section = next ? rest.slice(0, next.index) : rest
  const after = next ? rest.slice(next.index) : ''

  return {
    body: (text.slice(0, from) + after).replace(/\n{3,}/g, '\n\n').trim(),
    instrumentation: section.trim() || null,
  }
}

/** `fun twoSum(nums: IntArray, ...): IntArray` → `twoSum` */
export function functionName(signature: string): string {
  return signature.slice(signature.indexOf(' ') + 1, signature.indexOf('(')).trim()
}
