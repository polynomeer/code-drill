/**
 * 기댓값과 실행값의 첫 차이 (UI 디자인 문서 §7.3 Diff Block — 긴 출력은 앞뒤 문맥과 첫 차이 위치).
 *
 * 값을 JSON 한 줄로 펴서 글자 단위로 견준다. 문자열 안의 탭·개행은 JSON 이 `\t`·`\n` 으로
 * 드러내므로, 눈에 보이지 않는 공백 차이가 따로 토글 없이도 보인다.
 */
export type Segment = { text: string; differs: boolean }

export type DiffView = {
  expected: Segment[]
  actual: Segment[]
  /** 처음 갈라진 글자 위치(펴 놓은 글 기준). 같으면 null */
  at: number | null
  /** 둘 다 배열이고 길이가 다르면 그 길이 */
  lengths: { expected: number; actual: number } | null
}

/** 차이 앞뒤로 보일 글자 수. 이보다 길면 "…" 로 접는다. */
const CONTEXT = 40

export function diffValues(expected: unknown, actual: unknown, raw?: string): DiffView {
  const left = flatten(expected)
  const right = raw ?? flatten(actual)
  const at = firstDifference(left, right)
  const lengths =
    Array.isArray(expected) && Array.isArray(actual) && expected.length !== actual.length
      ? { expected: expected.length, actual: actual.length }
      : null
  return { expected: around(left, at), actual: around(right, at), at, lengths }
}

export function flatten(value: unknown): string {
  return value === undefined ? '' : JSON.stringify(value)
}

export function firstDifference(a: string, b: string): number | null {
  if (a === b) return null
  const length = Math.min(a.length, b.length)
  for (let i = 0; i < length; i++) if (a[i] !== b[i]) return i
  return length
}

/**
 * 첫 차이를 가운데 두고 문맥만 남긴다. 차이부터 끝까지가 "다른 부분"이다 — 한 글자가 밀리면
 * 뒤가 전부 어긋나므로, 그 뒤를 정확히 맞추려 하기보다 "여기서부터 다르다"를 보인다.
 */
export function around(text: string, at: number | null): Segment[] {
  if (at === null) return trimmed(text, 0, false)
  const start = Math.max(0, at - CONTEXT)
  const end = Math.min(text.length, at + CONTEXT)
  const segments: Segment[] = []
  if (start > 0) segments.push({ text: '…', differs: false })
  segments.push({ text: text.slice(start, at), differs: false })
  segments.push({ text: text.slice(at, end) || '∅', differs: true })
  if (end < text.length) segments.push({ text: '…', differs: false })
  return segments.filter((segment) => segment.text !== '')
}

function trimmed(text: string, from: number, differs: boolean): Segment[] {
  const limit = CONTEXT * 2
  return text.length - from > limit
    ? [{ text: text.slice(from, from + limit), differs }, { text: '…', differs: false }]
    : [{ text: text.slice(from), differs }]
}
