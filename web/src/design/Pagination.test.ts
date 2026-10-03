import { describe, expect, it } from 'vitest'
import { pages } from './Pagination'

describe('pages', () => {
  it('적으면 다 보인다', () => {
    expect(pages(2, 4)).toEqual([1, 2, 3, 4])
  })

  it('많으면 처음·끝·지금 양옆만 보이고 사이를 접는다', () => {
    expect(pages(6, 12)).toEqual([1, 'gap', 5, 6, 7, 'gap', 12])
  })

  it('하나만 빠지면 접지 않고 그 숫자를 보인다', () => {
    expect(pages(4, 12)).toEqual([1, 2, 3, 4, 5, 'gap', 12])
  })

  it('끝 쪽에서도 같다', () => {
    expect(pages(12, 12)).toEqual([1, 'gap', 11, 12])
  })
})
