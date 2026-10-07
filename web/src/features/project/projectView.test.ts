import { describe, expect, it } from 'vitest'
import { fileTree, firstFile, hiddenCells, judgingStep, statementBody } from './projectView'

describe('fileTree', () => {
  it('폴더 줄을 만들고, 같은 폴더에서는 하위 폴더가 파일보다 먼저다', () => {
    const rows = fileTree(['src/queue/Lease.kt', 'README.md', 'src/queue/JobQueue.kt', 'tests/PublicJobQueueTest.kt', 'src/Main.kt'])
    expect(rows.map((row) => `${'  '.repeat(row.depth)}${row.name}${row.folder ? '/' : ''}`)).toEqual([
      'src/',
      '  queue/',
      '    JobQueue.kt',
      '    Lease.kt',
      '  Main.kt',
      'tests/',
      '  PublicJobQueueTest.kt',
      'README.md',
    ])
  })
})

describe('firstFile', () => {
  it('TODO 가 있는 소스를 먼저 연다', () => {
    expect(firstFile({ 'tests/test_a.py': 'TODO', 'cart/__init__.py': '', 'cart/pricing.py': '# TODO', 'cart/rules.py': '' })).toBe('cart/pricing.py')
  })

  it('TODO 가 없으면 테스트·패키지 선언이 아닌 첫 파일', () => {
    expect(firstFile({ 'tests/test_a.py': '', 'cart/__init__.py': '', 'cart/rules.py': '' })).toBe('cart/rules.py')
    expect(firstFile({})).toBeNull()
  })
})

describe('judgingStep', () => {
  it('서버의 세 상태가 세 단계다', () => {
    expect(['QUEUED', 'LEASED', 'COMPLETED'].map((status) => judgingStep(status as never))).toEqual([0, 1, 2])
  })
})

describe('hiddenCells', () => {
  it('통과 수만큼 앞에서부터, 범위를 넘지 않게', () => {
    expect(hiddenCells(2, 4)).toEqual([true, true, false, false])
    expect(hiddenCells(9, 3)).toEqual([true, true, true])
    expect(hiddenCells(null, null)).toEqual([])
  })
})

describe('statementBody', () => {
  it('첫 줄의 제목만 뗀다', () => {
    expect(statementBody('# 작업 큐\n\n본문', '작업 큐')).toBe('본문')
    expect(statementBody('# 다른 제목\n본문', '작업 큐')).toBe('# 다른 제목\n본문')
  })
})
