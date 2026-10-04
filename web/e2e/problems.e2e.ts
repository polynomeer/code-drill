import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

/**
 * P-01 문제 탐색 `/problems` 와 읽기 `/problems/:slug` (docs/ui-overhaul.md §6.1) — 로그인 없이.
 *
 * API 를 흉내 낸다. 쪽·정렬은 서버가 하므로(ProblemControllerTest) 여기서는 화면이 주소와 요청을
 * 맞게 만드는지, 표·빈 결과·읽기 화면이 그려지는지를 본다.
 */
const ITEMS = Array.from({ length: 3 }, (_, index) => ({
  id: `p${index}`,
  number: 1000 + index,
  version: 1,
  title: `문제 ${index}`,
  difficulty: (['EASY', 'MEDIUM', 'HARD'] as const)[index],
  tags: ['array', index === 2 ? 'graph' : 'math'],
  competencies: [],
  solvedRate: index === 0 ? null : 0.5,
  solvedCount: index * 10,
  solved: false,
}))

const DETAIL = {
  id: 'p0',
  number: 1000,
  version: 1,
  title: '문제 0',
  statement: '# 문제 0\n\n배열을 **뒤집는다**.',
  timeMillis: 1000,
  memoryMb: 256,
  signature: 'fun f(a: IntArray): IntArray',
  samples: [{ id: 's1', args: [[1, 2]], expected: [2, 1] }],
  groups: [{ id: 'sample', weight: 100, aggregation: 'ALL_OR_NOTHING', caseCount: 1 }],
}

async function mockApi(page: Page, requests: URL[] = []) {
  await page.route('**/api/v1/**', async (route: Route) => {
    const url = new URL(route.request().url())
    const path = url.pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (path === '/problems') {
      requests.push(url)
      const query = url.searchParams.get('query')
      const items = query === 'nothing' ? [] : ITEMS
      return json({ items, nextCursor: null, total: items.length === 0 ? 0 : 120, tags: { array: 3, math: 2, graph: 1 }, page: Number(url.searchParams.get('page')), pageCount: 3 })
    }
    if (path === '/problems/p0') return json(DETAIL)
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

test('번호·난이도·정답률이 있는 표를 그리고 표본이 적으면 정답률을 비운다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/problems')
  const table = page.getByRole('table')
  await expect(table.getByRole('row')).toHaveCount(ITEMS.length + 1)
  await expect(table.getByRole('link', { name: '문제 1' })).toHaveAttribute('href', '/problems/p1')
  await expect(table.getByRole('row').nth(1)).toContainText('1000')
  await expect(table.getByRole('row').nth(1)).toContainText('—')
  await expect(page.getByText('120문제')).toBeVisible()
  // 둘러보는 사람에게는 상태 필터가 없다
  await expect(page.getByRole('group', { name: '상태' })).toHaveCount(0)
})

test('정렬·쪽은 주소와 요청에 실린다', async ({ page, isMobile }) => {
  test.skip(isMobile, '정답률 열은 모바일에서 접힌다')
  const requests: URL[] = []
  await mockApi(page, requests)
  await page.goto('/problems')
  await page.getByRole('button', { name: /정답률/ }).click()
  await expect(page).toHaveURL(/sort=ACCURACY/)
  await expect(page.getByRole('columnheader', { name: /정답률/ })).toHaveAttribute('aria-sort', 'ascending')
  await page.getByRole('button', { name: /정답률/ }).click()
  await expect(page).toHaveURL(/order=DESC/)

  await page.getByRole('button', { name: '2쪽' }).click()
  await expect(page).toHaveURL(/page=2/)
  await expect.poll(() => requests.at(-1)?.search).toContain('page=2')
  expect(requests.at(-1)?.searchParams.get('sort')).toBe('ACCURACY')
  expect(requests.at(-1)?.searchParams.get('order')).toBe('DESC')
})

test('필터를 바꾸면 1쪽으로 돌아간다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/problems?page=3')
  await page.getByRole('button', { name: '어려움' }).click()
  await expect(page).toHaveURL(/difficulty=HARD/)
  await expect(page).not.toHaveURL(/page=/)
})

test('빈 결과는 걸린 조건과 초기화를 보인다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/problems?query=nothing')
  await expect(page.getByText('조건에 맞는 문제가 없습니다')).toBeVisible()
  await expect(page.getByText('검색: nothing')).toBeVisible()
  await page.getByRole('button', { name: '필터 초기화' }).click()
  await expect(page).not.toHaveURL(/query=/)
})

test('읽기 화면은 로그인 없이 열리고 풀기도 로그인 없이 풀이 화면으로 간다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/problems/p0')
  await expect(page.getByRole('heading', { level: 1 })).toContainText('1000')
  await expect(page.getByRole('article', { name: '문제' })).toContainText('뒤집는다')
  await expect(page.getByRole('link', { name: '풀어 보기' })).toHaveAttribute('href', '/problems/p0/solve')
})

test('접근성 위반이 없다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/problems')
  await expect(page.getByRole('table')).toBeVisible()
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})
