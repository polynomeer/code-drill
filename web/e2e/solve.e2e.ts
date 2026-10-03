import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * S-01 풀이 Workspace (docs/ui-overhaul.md §6.2) — API 를 흉내 내고 화면만 본다.
 *
 * 채점까지 도는 흐름은 앱 셋이 떠 있어야 하고 scripts/smoke.py 가 API 로 확인한다. 여기서는
 * 레이아웃·지문 렌더링·단축키·접근성처럼 화면이 책임지는 것만 본다. 응답은 아래 PROBLEM 처럼
 * 실제 API 모양 그대로 둔다 — 모양이 바뀌면 타입(shared/types.ts)과 함께 고친다.
 */
const PROBLEM = {
  id: 'apply-range-updates',
  version: 1,
  title: '구간 더하기 뒤의 배열',
  statement: [
    '# 구간 더하기 뒤의 배열',
    '',
    '길이 `n` 의 0 배열에 갱신을 **차례로** 적용한다.',
    '',
    '```kotlin',
    'fun applyRangeUpdates(n: Int, updates: IntArray): IntArray',
    '```',
    '',
    '## 실행 리플레이 계측 (선택)',
    '',
    '```kotlin',
    'Drill.write(l, v)',
    '```',
    '',
    '## 제약',
    '',
    '- `1 <= n <= 100_000`',
  ].join('\n'),
  timeMillis: 2000,
  memoryMb: 256,
  signature: 'fun applyRangeUpdates(n: Int, updates: IntArray): IntArray',
  samples: [{ id: 's1', args: [5, [1, 3, 2]], expected: [0, 2, 2, 2, 0] }],
  groups: [{ id: 'sample', weight: 0, aggregation: 'ALL_OR_NOTHING', caseCount: 1 }],
}

async function mockApi(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem(
      'codedrill.session',
      JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }),
    )
  })
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

    if (path === `/problems/${PROBLEM.id}`) return json(PROBLEM)
    if (path === '/submissions' && route.request().method() === 'GET') return json({ items: [], nextCursor: null })
    if (path.startsWith('/workspaces/')) return route.fulfill({ status: 204 })
    if (path.startsWith('/prequestions/')) return json({ questions: [], answered: [] })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

test.beforeEach(async ({ page }) => {
  await mockApi(page)
})

test('지문을 마크다운으로 그리고 계측 절은 리플레이 탭으로 옮긴다', async ({ page, isMobile }) => {
  await page.goto(`/problems/${PROBLEM.id}/solve`)
  const statement = page.getByRole('article', { name: '문제' })
  await expect(statement.getByText('차례로')).toBeVisible()
  // 원문 마크다운 기호가 보이지 않는다
  await expect(statement).not.toContainText('**')
  await expect(statement).not.toContainText('```')
  // 제목은 툴바가 그린다 — 본문에 같은 H1 이 또 나오지 않는다
  await expect(page.getByRole('heading', { level: 1, name: PROBLEM.title })).toHaveCount(1)
  // 계측 절은 지문에 없다
  await expect(statement).not.toContainText('Drill.write')
  // 예제는 표로
  await expect(statement.getByRole('table').first()).toContainText('[0,2,2,2,0]')

  if (isMobile) await page.getByRole('tab', { name: '결과' }).click()
  await page.getByRole('tab', { name: '리플레이' }).click()
  await page.getByText('리플레이 계측 (선택)').click()
  await expect(page.getByText('Drill.write(l, v)')).toBeVisible()
})

test('언어는 주소에 남는다', async ({ page }) => {
  await page.goto(`/problems/${PROBLEM.id}/solve?lang=PYTHON`)
  await expect(page.getByLabel('언어')).toHaveValue('PYTHON')
  await page.getByLabel('언어').selectOption('JAVA')
  await expect(page).toHaveURL(/lang=JAVA/)
})

test.describe('데스크톱', () => {
  test.skip(({ isMobile }) => isMobile, '분할 창은 768px 이상에서만')

  test('⌘J 로 결과 창을 접고 펼친다', async ({ page }) => {
    await page.goto(`/problems/${PROBLEM.id}/solve`)
    const separator = page.getByRole('separator', { name: '에디터와 결과 창 사이 크기 조절' })
    await expect(separator).toBeVisible()
    const before = Number(await separator.getAttribute('aria-valuenow'))
    await page.keyboard.press('ControlOrMeta+j')
    await expect.poll(async () => Number(await separator.getAttribute('aria-valuenow'))).toBeGreaterThan(before + 10)
    await page.keyboard.press('ControlOrMeta+j')
    await expect.poll(async () => Number(await separator.getAttribute('aria-valuenow'))).toBeCloseTo(before, 0)
  })

  test('문제 창 접기 버튼과 ⌥1 로 다시 펼치기', async ({ page }) => {
    await page.goto(`/problems/${PROBLEM.id}/solve`)
    const separator = page.getByRole('separator', { name: '문제와 에디터 사이 크기 조절' })
    await page.getByRole('button', { name: /문제 창 접기/ }).click()
    await expect(separator).toHaveAttribute('aria-valuenow', '0')
    await page.keyboard.press('Alt+1')
    await expect.poll(async () => Number(await separator.getAttribute('aria-valuenow'))).toBeGreaterThan(20)
    await expect(page.getByRole('region', { name: /문제 창/ })).toBeFocused()
  })

  test('접근성 위반이 없다', async ({ page }) => {
    await page.goto(`/problems/${PROBLEM.id}/solve`)
    await expect(page.getByRole('article', { name: '문제' })).toBeVisible()
    const result = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      // Monaco 는 우리가 그리는 DOM 이 아니다. 접근성은 Monaco 자신의 스크린리더 모드가 맡는다.
      .exclude('.monaco-editor')
      .analyze()
    expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
})

test('모바일은 문제·코드·결과를 탭으로 바꾼다', async ({ page, isMobile }) => {
  test.skip(!isMobile, '모바일 전용')
  await page.goto(`/problems/${PROBLEM.id}/solve`)
  await expect(page.getByRole('tab', { name: '문제', exact: true }).first()).toHaveAttribute('aria-selected', 'true')
  await page.getByRole('tab', { name: '코드' }).click()
  await expect(page.getByText('긴 코드는 데스크톱에서 쓰기 편합니다.')).toBeVisible()
  const width = page.viewportSize()?.width ?? 0
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width)
})
