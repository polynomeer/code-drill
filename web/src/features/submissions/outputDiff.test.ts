import { describe, expect, it } from 'vitest'
import { around, diffValues, firstDifference } from './outputDiff'

describe('firstDifference', () => {
  it('같으면 null, 한쪽이 짧으면 짧은 끝', () => {
    expect(firstDifference('[0,1]', '[0,1]')).toBeNull()
    expect(firstDifference('[0,1]', '[0,2]')).toBe(3)
    expect(firstDifference('[0]', '[0,1]')).toBe(2)
  })
})

describe('diffValues', () => {
  it('첫 차이부터를 다른 부분으로 표시한다', () => {
    const view = diffValues([0, 1], [0, 2])
    expect(view.at).toBe(3)
    expect(view.expected).toEqual([
      { text: '[0,', differs: false },
      { text: '1]', differs: true },
    ])
    expect(view.actual.find((s) => s.differs)?.text).toBe('2]')
  })

  it('배열 길이가 다르면 길이를 알린다', () => {
    expect(diffValues([1, 2, 3], [1]).lengths).toEqual({ expected: 3, actual: 1 })
    expect(diffValues([1, 2], [1, 3]).lengths).toBeNull()
  })

  it('문자열의 탭·개행은 JSON 이 드러낸다', () => {
    const view = diffValues('a b', 'a\tb')
    expect(view.actual.map((s) => s.text).join('')).toContain('\\t')
  })

  it('되돌리지 못한 원문은 그대로 견준다', () => {
    const view = diffValues([1, 2], undefined, '1,x')
    expect(view.actual.map((s) => s.text).join('')).toBe('1,x')
  })
})

describe('around', () => {
  it('긴 글은 첫 차이 앞뒤만 남긴다', () => {
    const long = 'a'.repeat(100) + 'X' + 'b'.repeat(100)
    const segments = around(long, 100)
    expect(segments[0]).toEqual({ text: '…', differs: false })
    expect(segments.at(-1)).toEqual({ text: '…', differs: false })
    expect(segments.find((s) => s.differs)?.text.startsWith('X')).toBe(true)
  })

  it('끝에서 모자라면 빈 자리를 보인다', () => {
    expect(around('[0]', 3).find((s) => s.differs)?.text).toBe('∅')
  })
})
