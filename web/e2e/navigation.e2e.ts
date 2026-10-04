import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 전역 탐색 (docs/ui-overhaul.md §4) — ⌘K 검색, 모바일 하단 탭, 알림. API 를 흉내 낸다.
 */
const problem = (number: number, id: string, title: string, tags: string[]) => ({
  id,
  number,
  version: 1,
  title,
  difficulty: 'EASY',
  tags,
  competencies: [],
  solvedRate: null,
  solvedCount: 0,
  solved: false,
})

const PROBLEMS = [problem(1000, 'two-sum', '두 수의 합', ['hash']), problem(1042, 'merge-intervals', '구간 합치기', ['sort'])]

async function mockApi(page: Page, options: { signedIn?: boolean } = {}) {
  const calls: string[] = []
  let unread = 2
  if (options.signedIn !== false) {
    await page.addInitScript(() => {
      localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
    })
  }
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    calls.push(`${request.method()} ${path}`)
    if (path === '/problems') return json({ items: PROBLEMS, nextCursor: null, total: 2, tags: {}, page: 1, pageCount: 1 })
    if (path === '/problems/two-sum')
      return json({ ...PROBLEMS[0], statement: '# 두 수의 합', timeMillis: 1000, memoryMb: 256, signature: 'fun twoSum(nums: IntArray, target: Int): IntArray', samples: [], groups: [] })
    if (path === '/me/notifications')
      return json({
        unread,
        items: [
          { id: 'answer:a1', kind: 'ANSWERED', title: '내 질문에 답이 달렸습니다', body: '왜 해시인가요', link: '/problems/two-sum/solve', at: new Date().toISOString(), unread: unread > 0 },
          { id: 'rating:c1', kind: 'RATING_CHANGED', title: '주간 대회 — 3위, 레이팅 +12', body: '1500 → 1512', link: '/contests/c1', at: '2026-10-01T00:00:00Z', unread: unread > 0 },
        ],
      })
    if (path === '/me/notifications/read') {
      unread = 0
      return route.fulfill({ status: 204 })
    }
    if (path === '/submissions' && request.method() === 'POST')
      return json({ id: 's1', problemId: 'two-sum', problemVersion: 1, language: 'KOTLIN', status: 'QUEUED', verdict: null, score: null, groups: null, compileLog: null, revision: 1, createdAt: new Date().toISOString(), mine: true })
    if (path === '/submissions' && request.method() === 'GET') return json({ items: [], nextCursor: null })
    if (path === '/submissions/s1')
      return json({ id: 's1', problemId: 'two-sum', problemVersion: 1, language: 'KOTLIN', status: 'COMPLETED', verdict: 'ACCEPTED', score: 100, groups: [], compileLog: null, revision: 1, createdAt: new Date().toISOString(), mine: true })
    if (path.startsWith('/submissions/s1/')) return route.fulfill({ status: 404 })
    if (path === '/me/prescription') return json({ date: '', items: [], streak: { days: 0, activeToday: false, atRisk: false } })
    if (path === '/events') return route.fulfill({ status: 202 })
    if (path.startsWith('/workspaces/') || path === '/me/onboarding') return route.fulfill({ status: 204 })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return calls
}

test.describe('⌘K 검색', () => {
  test('번호로 찾아 Enter 로 풀이 화면에 간다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/problems')
    await expect(page.getByRole('link', { name: '두 수의 합' })).toBeVisible()
    await page.keyboard.press('ControlOrMeta+k')
    const input = page.getByRole('combobox', { name: /문제 번호·제목·태그/ })
    await expect(input).toBeFocused()
    await input.fill('1042')
    const first = page.getByRole('option').first()
    await expect(first).toContainText('구간 합치기')
    await expect(first).toHaveAttribute('aria-selected', 'true')
    await expect(input).toHaveAttribute('aria-activedescendant', (await first.getAttribute('id'))!)
    await page.keyboard.press('Enter')
    await expect(page).toHaveURL(/\/problems\/merge-intervals\/solve$/)
  })

  test('`/` 로도 열리고, 명령으로 테마를 바꾸고, Esc 로 닫는다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/problems')
    await page.locator('body').press('/')
    const input = page.getByRole('combobox')
    await input.fill('다크')
    await expect(page.getByRole('option').first()).toContainText('테마: 다크')
    await page.keyboard.press('Enter')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    await page.keyboard.press('ControlOrMeta+k')
    await expect(page.getByRole('combobox')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('combobox')).toHaveCount(0)
  })

  test('↓ 로 고르면 고른 줄이 옮겨 가고, 맞는 것이 없으면 그렇게 말한다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/problems')
    await page.getByRole('button', { name: '검색 (⌘K)' }).click()
    const input = page.getByRole('combobox')
    await input.fill('합')
    await expect(page.getByRole('option')).toHaveCount(2)
    await page.keyboard.press('ArrowDown')
    await expect(page.getByRole('option').nth(1)).toHaveAttribute('aria-selected', 'true')
    await input.fill('없는말zz')
    await expect(page.getByText('"없는말zz" 에 맞는 것이 없습니다')).toBeVisible()
  })

  test('검색 창에 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/problems')
    await page.keyboard.press('ControlOrMeta+k')
    await page.getByRole('combobox').fill('합')
    await expect(page.getByRole('option').first()).toBeVisible()
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
    expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
})

test('키보드만으로 문제를 찾아 제출하고 판정을 본다 (디자인 설계서 §16.3)', async ({ page, isMobile }) => {
  test.skip(isMobile, '물리 키보드가 있는 화면')
  const calls = await mockApi(page)
  await page.goto('/problems')
  await expect(page.getByRole('link', { name: '두 수의 합' })).toBeVisible()
  // 찾기 — ⌘K 와 번호, Enter
  await page.keyboard.press('ControlOrMeta+k')
  await page.keyboard.type('1000')
  await page.keyboard.press('Enter')
  await expect(page).toHaveURL(/\/problems\/two-sum\/solve$/)
  await expect(page.locator('.monaco-editor .view-lines')).toBeVisible({ timeout: 15_000 })
  // 편집기로 (⌥2), 제출 (⌘⇧↵)
  await page.keyboard.press('Alt+2')
  await page.keyboard.press('ControlOrMeta+Shift+Enter')
  await expect.poll(() => calls).toContain('POST /submissions')
  await expect(page.getByText('맞았습니다').first()).toBeVisible()
})

test.describe('알림', () => {
  test('안 읽은 수를 단추 이름에 싣고, 열면 읽음이 된다', async ({ page, isMobile }) => {
    const calls = await mockApi(page)
    await page.goto('/problems')
    const bell = page.getByRole('button', { name: '알림, 안 읽은 것 2개' })
    await expect(bell).toBeVisible()
    await bell.click()
    await expect(page.getByRole('link', { name: /새 알림: 내 질문에 답이 달렸습니다/ })).toBeVisible()
    await expect.poll(() => calls).toContain('POST /me/notifications/read')
    await expect(page.getByRole('button', { name: '알림', exact: true })).toBeVisible()
    await page.getByRole('link', { name: /주간 대회 — 3위/ }).click()
    await expect(page).toHaveURL(/\/contests\/c1$/)
    if (!isMobile) await expect(page.locator('#notifications')).not.toBeVisible()
  })
})

test.describe('모바일 하단 탭', () => {
  test.skip(({ isMobile }) => !isMobile, '좁은 화면 전용')

  test('로그인했으면 위에는 로고·검색·알림, 아래에 넷 — 나머지는 "나"에서', async ({ page }) => {
    await mockApi(page)
    await page.goto('/problems')
    const tabs = page.getByRole('navigation', { name: '하단 메뉴' })
    await expect(tabs.getByRole('link')).toHaveText(['문제', '훈련', '역량', '나'])
    await expect(tabs.getByRole('link', { name: '문제' })).toHaveAttribute('aria-current', 'page')
    await expect(page.getByRole('navigation', { name: '주 메뉴' })).toBeHidden()

    await tabs.getByRole('link', { name: '나' }).click()
    await expect(page).toHaveURL(/\/me$/)
    await expect(tabs.getByRole('link', { name: '나' })).toHaveAttribute('aria-current', 'page')
    for (const name of ['오늘 — 처방 요약·프로젝트·문제집', '대회', '내 제출', '내 프로필']) {
      await expect(page.getByRole('link', { name })).toBeVisible()
    }
    await page.getByRole('button', { name: '다크' }).click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await page.getByRole('button', { name: '계정 설정' }).click()
    await expect(page.getByRole('dialog', { name: '계정 설정' })).toBeVisible()
  })

  test('둘러보는 사람에게는 하단 탭이 없고, 풀이 화면에서도 없다', async ({ page }) => {
    await mockApi(page, { signedIn: false })
    await page.goto('/problems')
    await expect(page.getByRole('navigation', { name: '하단 메뉴' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: '로그인', exact: true })).toBeVisible()
  })

  test('하단 탭이 본문을 가리지 않고 넘치지 않으며 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/me')
    await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
    // 마지막 단추까지 스크롤하면 탭 위로 보인다
    await page.getByRole('button', { name: '로그아웃' }).scrollIntoViewIfNeeded()
    const logout = await page.getByRole('button', { name: '로그아웃' }).boundingBox()
    const bar = await page.getByRole('navigation', { name: '하단 메뉴' }).boundingBox()
    expect(logout!.y + logout!.height).toBeLessThanOrEqual(bar!.y)
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
    expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
})
