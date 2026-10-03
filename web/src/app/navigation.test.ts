import { describe, expect, it } from 'vitest'
import { safeNext } from './navigation'

describe('safeNext', () => {
  it('사이트 안의 경로는 그대로 따른다', () => {
    expect(safeNext('/problems/two-sum/solve?lang=JAVA')).toBe('/problems/two-sum/solve?lang=JAVA')
  })

  it('밖으로 나가는 주소는 홈으로 바꾼다', () => {
    for (const raw of ['//evil.example', 'https://evil.example', '/\\evil.example', 'javascript:alert(1)', '', null]) {
      expect(safeNext(raw)).toBe('/')
    }
  })
})
