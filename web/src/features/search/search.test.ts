import { describe, expect, it } from 'vitest'
import { search } from './search'
import type { Command } from './search'
import type { ProblemSummary } from '../../shared/types'

const problem = (number: number, id: string, title: string, tags: string[] = []): ProblemSummary =>
  ({ id, number, title, tags, version: 1, difficulty: 'EASY', competencies: [], solvedRate: null, solvedCount: 0, solved: false }) as ProblemSummary

const PROBLEMS = [
  problem(1000, 'two-sum', '두 수의 합', ['hash']),
  problem(1001, 'max-subarray', '최대 부분합', ['dp']),
  problem(1042, 'merge-intervals', '구간 합치기', ['sort']),
  problem(1100, 'sum-of-digits', '자릿수 합', ['math']),
  problem(1200, 'sum-tree', '합 트리', ['tree']),
]

const command = (label: string, keywords: string[] = []): Command => ({ id: label, label, keywords, group: '이동', run: () => undefined })
const COMMANDS = [command('훈련', ['training']), command('역량', ['competency']), command('다크 테마', ['dark'])]

const ids = (query: string) =>
  search(query, PROBLEMS, COMMANDS).map((hit) => (hit.kind === 'problem' ? hit.problem.id : `cmd:${hit.command.label}`))

describe('전역 검색 순위', () => {
  it('번호를 치면 그 번호의 문제가 맨 위다', () => {
    expect(ids('1042')[0]).toBe('merge-intervals')
    expect(ids('10')).toEqual(['two-sum', 'max-subarray', 'merge-intervals'])
  })

  it('제목이 그 말로 시작하면 먼저, 같은 순위끼리는 번호 순', () => {
    expect(ids('합')).toEqual(['sum-tree', 'two-sum', 'max-subarray', 'merge-intervals', 'sum-of-digits'])
  })

  it('slug 와 태그로도 찾는다', () => {
    expect(ids('two sum')).toEqual(['two-sum'])
    expect(ids('dp')).toEqual(['max-subarray'])
  })

  it('비면 명령만, 명령 이름이 맞으면 명령이 위', () => {
    expect(ids('')).toEqual(['cmd:훈련', 'cmd:역량', 'cmd:다크 테마'])
    expect(ids('dark')[0]).toBe('cmd:다크 테마')
  })
})
