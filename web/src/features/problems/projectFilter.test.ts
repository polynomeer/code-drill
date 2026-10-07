import { describe, expect, it } from 'vitest'
import type { ProjectSummary } from '../../shared/types'
import { EMPTY_PROJECT_FILTER, filterProjects, projectHref, projectLanguages } from './projectFilter'

const project = (id: string, language: string, difficulty: ProjectSummary['difficulty']): ProjectSummary => ({
  id,
  version: 1,
  title: id,
  language,
  difficulty,
  tags: [],
  summary: '',
  solved: false,
})

const ITEMS = [project('a', 'PYTHON', 'EASY'), project('b', 'KOTLIN', 'MEDIUM'), project('c', 'PYTHON', 'MEDIUM')]

describe('filterProjects', () => {
  it('조건이 없으면 전부', () => {
    expect(filterProjects(ITEMS, EMPTY_PROJECT_FILTER)).toHaveLength(3)
  })

  it('묶음 안은 또는, 묶음끼리는 그리고', () => {
    expect(filterProjects(ITEMS, { languages: ['PYTHON', 'KOTLIN'], difficulty: [] }).map((p) => p.id)).toEqual(['a', 'b', 'c'])
    expect(filterProjects(ITEMS, { languages: ['PYTHON'], difficulty: ['MEDIUM'] }).map((p) => p.id)).toEqual(['c'])
  })
})

describe('projectLanguages', () => {
  it('있는 언어만, Kotlin·Java·Python 순서로', () => {
    expect(projectLanguages(ITEMS)).toEqual(['KOTLIN', 'PYTHON'])
  })
})

describe('projectHref', () => {
  it('둘러보는 사람은 로그인을 거쳐 작업 공간으로 돌아온다', () => {
    expect(projectHref('cart-pricing', true)).toBe('/?project=cart-pricing')
    expect(projectHref('cart-pricing', false)).toBe('/login?next=%2F%3Fproject%3Dcart-pricing')
  })
})
