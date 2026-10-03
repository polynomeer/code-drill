import { describe, expect, it } from 'vitest'
import { decodeWire, returnKind, showWire } from './wire'

const b64 = (text: string) => btoa(String.fromCharCode(...new TextEncoder().encode(text)))

describe('returnKind', () => {
  it('시그니처의 반환형을 읽는다', () => {
    expect(returnKind('fun twoSum(nums: IntArray, target: Int): IntArray')).toBe('INT_ARRAY')
    expect(returnKind('fun f(grid: Array<IntArray>): Array<IntArray>')).toBe('INT_MATRIX')
    expect(returnKind('fun f(words: Array<String>): Array<String>')).toBe('STRING_ARRAY')
    expect(returnKind('fun f(s: String): Int')).toBe('INT')
    expect(returnKind('fun f(): Unknown')).toBeNull()
  })
})

describe('decodeWire', () => {
  it('정수와 정수 배열', () => {
    expect(showWire('INT', '-7')).toBe('-7')
    expect(showWire('INT_ARRAY', '0,1')).toBe('[0,1]')
    expect(showWire('INT_ARRAY', '')).toBe('[]')
  })

  it('문자열은 Base64 를 풀고 한글·탭·쉼표를 지킨다', () => {
    expect(decodeWire('STRING', b64('가\t나,다'))).toEqual({ ok: true, value: '가\t나,다' })
  })

  it('문자열 배열은 개수로 빈 배열과 빈 문자열 하나를 가른다', () => {
    expect(decodeWire('STRING_ARRAY', '0')).toEqual({ ok: true, value: [] })
    expect(decodeWire('STRING_ARRAY', '1,')).toEqual({ ok: true, value: [''] })
    expect(decodeWire('STRING_ARRAY', `2,${b64('a')},${b64('b')}`)).toEqual({ ok: true, value: ['a', 'b'] })
  })

  it('격자는 행 우선으로 다시 접는다', () => {
    expect(decodeWire('INT_MATRIX', '2,3,1,2,3,4,5,6')).toEqual({ ok: true, value: [[1, 2, 3], [4, 5, 6]] })
    expect(decodeWire('INT_MATRIX', '0,0')).toEqual({ ok: true, value: [] })
  })

  it('되돌리지 못하면 원문을 그대로 보인다', () => {
    expect(showWire('INT_ARRAY', '1,x')).toBe('1,x')
    expect(showWire('INT_MATRIX', '2,2,1')).toBe('2,2,1')
    expect(showWire(null, 'whatever')).toBe('whatever')
  })
})
