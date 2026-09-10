/**
 * 두 제출의 코드 차이 (PRD §6.4).
 *
 * 줄 단위 LCS 다. 라이브러리를 들이지 않는 이유는 이것 하나 때문에 번들이 커지는 것이
 * 아깝기 때문이며(web/scripts/bundle-budget.mjs), 풀이 코드는 길어야 수백 줄이라 O(n·m)
 * 로 충분하다.
 *
 * **문자 단위로 내려가지 않는다.** 한 줄 안에서 무엇이 바뀌었는지까지 보여주면 정확하지만,
 * 사용자가 두 제출을 비교하며 찾는 것은 대개 "어느 줄을 고쳤나"이고 그 답은 줄 단위로
 * 이미 나온다.
 */
export type DiffKind = 'same' | 'added' | 'removed'

export interface DiffLine {
  kind: DiffKind
  text: string
  /** 각 쪽의 줄 번호. 그쪽에 없는 줄이면 null 이다. */
  left: number | null
  right: number | null
}

/** 너무 큰 입력에서 LCS 표가 메모리를 먹지 않도록 둔 한계. */
const MAX_LINES = 2000

export function diffLines(before: string, after: string): DiffLine[] {
  const a = before.split('\n')
  const b = after.split('\n')

  if (a.length > MAX_LINES || b.length > MAX_LINES) {
    // 비교를 포기하되 조용히 빈 화면을 주지는 않는다. 양쪽을 통째로 보여준다.
    return [
      ...a.map((text, i) => ({ kind: 'removed' as const, text, left: i + 1, right: null })),
      ...b.map((text, i) => ({ kind: 'added' as const, text, left: null, right: i + 1 })),
    ]
  }

  // lcs[i][j] = a[i..] 와 b[j..] 의 최장 공통 부분 수열 길이.
  const lcs: number[][] = Array.from({ length: a.length + 1 }, () =>
    new Array<number>(b.length + 1).fill(0),
  )
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      lcs[i]![j] = a[i] === b[j] ? lcs[i + 1]![j + 1]! + 1 : Math.max(lcs[i + 1]![j]!, lcs[i]![j + 1]!)
    }
  }

  const out: DiffLine[] = []
  let i = 0
  let j = 0
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      out.push({ kind: 'same', text: a[i]!, left: i + 1, right: j + 1 })
      i++
      j++
    } else if (lcs[i + 1]![j]! >= lcs[i]![j + 1]!) {
      out.push({ kind: 'removed', text: a[i]!, left: i + 1, right: null })
      i++
    } else {
      out.push({ kind: 'added', text: b[j]!, left: null, right: j + 1 })
      j++
    }
  }
  while (i < a.length) out.push({ kind: 'removed', text: a[i]!, left: ++i, right: null })
  while (j < b.length) out.push({ kind: 'added', text: b[j]!, left: null, right: ++j })
  return out
}

/**
 * 바뀐 줄 주변만 남긴다.
 *
 * 200줄 중 두 줄이 바뀐 diff 를 전부 보여주면 바뀐 줄을 눈으로 찾아야 한다. 앞뒤
 * [context] 줄만 남기고 나머지는 접는다.
 */
export function collapse(lines: DiffLine[], context = 3): (DiffLine | { kind: 'gap'; count: number })[] {
  const keep = new Set<number>()
  lines.forEach((line, index) => {
    if (line.kind === 'same') return
    for (let k = index - context; k <= index + context; k++) {
      if (k >= 0 && k < lines.length) keep.add(k)
    }
  })

  const out: (DiffLine | { kind: 'gap'; count: number })[] = []
  let gap = 0
  lines.forEach((line, index) => {
    if (keep.has(index)) {
      if (gap > 0) {
        out.push({ kind: 'gap', count: gap })
        gap = 0
      }
      out.push(line)
    } else {
      gap++
    }
  })
  if (gap > 0) out.push({ kind: 'gap', count: gap })
  return out
}
