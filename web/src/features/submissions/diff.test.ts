import { describe, expect, it } from 'vitest'
import { collapse, diffLines } from './diff'

/**
 * 코드 diff (PRD §6.4).
 *
 * 두 제출을 비교하는 화면이 이것 위에 선다. 틀리면 사용자는 **고치지 않은 줄을 고쳤다고**
 * 읽게 되므로, 눈으로 보고 넘어갈 자리가 아니다.
 */
describe('diffLines', () => {
  const text = (lines: string[]) => lines.join('\n')

  it('같은 코드에는 바뀐 줄이 없다', () => {
    const result = diffLines(text(['a', 'b']), text(['a', 'b']))
    expect(result.every((line) => line.kind === 'same')).toBe(true)
  })

  it('가운데 한 줄을 고치면 그 줄만 바뀐다', () => {
    const result = diffLines(text(['a', 'b', 'c']), text(['a', 'B', 'c']))

    expect(result.map((line) => line.kind)).toEqual(['same', 'removed', 'added', 'same'])
    expect(result.filter((line) => line.kind !== 'same').map((line) => line.text)).toEqual(['b', 'B'])
  })

  it('줄을 끼워 넣으면 나머지는 그대로다', () => {
    // 삽입을 "그 뒤가 전부 바뀌었다"로 읽으면 diff 는 쓸모가 없다.
    const result = diffLines(text(['a', 'c']), text(['a', 'b', 'c']))

    expect(result.map((line) => line.kind)).toEqual(['same', 'added', 'same'])
  })

  it('줄 번호는 각자의 쪽에서 센다', () => {
    const result = diffLines(text(['a', 'b']), text(['a']))
    const removed = result.find((line) => line.kind === 'removed')

    expect(removed).toMatchObject({ text: 'b', left: 2, right: null })
  })

  it('한쪽이 비어도 답한다', () => {
    // 첫 제출과 비교하는 경우가 실제로 있다.
    expect(diffLines('', text(['a'])).map((l) => l.kind)).toEqual(['removed', 'added'])
  })
})

describe('collapse', () => {
  it('바뀐 줄에서 먼 구간은 접는다', () => {
    const before = Array.from({ length: 30 }, (_, i) => `line ${i}`).join('\n')
    const after = before.replace('line 15', 'CHANGED')

    const folded = collapse(diffLines(before, after), 2)
    const gaps = folded.filter((entry) => entry.kind === 'gap')

    expect(gaps.length).toBeGreaterThan(0)
    // 접힌 뒤에도 바뀐 줄은 남아 있어야 한다.
    expect(folded.some((e) => 'text' in e && e.text === 'CHANGED')).toBe(true)
    expect(folded.length).toBeLessThan(31)
  })

  it('바뀐 곳이 없으면 통째로 접는다', () => {
    const same = ['a', 'b', 'c'].join('\n')
    const folded = collapse(diffLines(same, same), 1)

    expect(folded).toEqual([{ kind: 'gap', count: 3 }])
  })
})
