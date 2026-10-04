import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 대회 (docs/ui-overhaul.md §6.6) — API 를 흉내 낸다.
 *
 * 닫는 조건: 진행 중 대회 카운트다운·순위표.
 */
const HOUR = 3600_000
const iso = (offset: number) => new Date(Date.now() + offset).toISOString()

const contest = (id: string, title: string, status: string, extra: Record<string, unknown> = {}) => ({
  id,
  kind: 'CONTEST',
  title,
  status,
  startsAt: status === 'SCHEDULED' ? iso(26 * HOUR) : iso(-HOUR),
  endsAt: status === 'SCHEDULED' ? iso(28 * HOUR) : status === 'RUNNING' ? iso(HOUR + 5 * 60_000) : iso(-HOUR / 2),
  minutes: 120,
  joined: false,
  entrants: 3,
  problemCount: 2,
  rated: true,
  ratedAt: null,
  ...extra,
})

const RUNNING = contest('c-run', '주간 대회 41', 'RUNNING', { joined: true })
const SCHEDULED = contest('c-next', '주간 대회 42', 'SCHEDULED')
const finished = (n: number) => contest(`c-old-${n}`, `지난 대회 ${n}`, 'FINISHED', { ratedAt: iso(-HOUR) })

const VIEW = {
  contest: RUNNING,
  problems: ['two-sum', 'bank-ledger'],
  standings: [
    { rank: 1, displayName: '에이다', mine: false, virtual: false, total: 200, solved: 2, lastSolvedAt: null, elapsedSeconds: 3930, perProblem: { 'two-sum': 100, 'bank-ledger': 100 }, ratingChange: null },
    { rank: 2, displayName: '시험', mine: true, virtual: false, total: 60, solved: 0, lastSolvedAt: null, elapsedSeconds: null, perProblem: { 'two-sum': 60 }, ratingChange: null },
    { rank: 3, displayName: '튜링', mine: false, virtual: false, total: 0, solved: 0, lastSolvedAt: null, elapsedSeconds: null, perProblem: {}, ratingChange: null },
  ],
  joinCode: null,
  virtual: null,
}

async function mockApi(page: Page) {
  const calls: string[] = []
  await page.addInitScript(() => {
    localStorage.setItem(
      'codedrill.session',
      JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }),
    )
  })
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    calls.push(`${request.method()} ${path}${url.search}`)

    if (path === '/problems')
      return json({ items: [{ id: 'two-sum', number: 1000, version: 1, title: '두 수의 합', difficulty: 'EASY', tags: [], competencies: [], solvedRate: null, solvedCount: 0, solved: false }], nextCursor: null, total: 1, tags: {}, page: 1, pageCount: 1 })
    if (path === '/projects') return json([{ id: 'bank-ledger', version: 1, title: '은행 장부', language: 'KOTLIN' }])
    if (path === '/contests' && url.searchParams.get('scope') === 'active') return json([RUNNING, SCHEDULED])
    if (path === '/contests/finished') {
      const pageNo = Number(url.searchParams.get('page') ?? 1)
      const all = Array.from({ length: 25 }, (_, i) => finished(i + 1))
      return json({ items: all.slice((pageNo - 1) * 20, pageNo * 20), page: pageNo, pageCount: 2, total: 25 })
    }
    if (path === '/contests/duels/join') return json({ contest: { ...RUNNING, id: 'duel-1', kind: 'DUEL', title: '대결' } })
    if (path === '/contests/c-next/join') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(SCHEDULED) })
    if (path === '/contests/c-run') return json(VIEW)
    if (path === '/contests/c-next') return json({ ...VIEW, contest: SCHEDULED, standings: [] })
    if (path === '/contests/c-old-1') return json({ ...VIEW, contest: finished(1), standings: VIEW.standings.map((s) => ({ ...s, ratingChange: s.mine ? -8 : 12 })) })
    if (path === '/contests/c-old-1/virtual') return json({ contest: { ...RUNNING, id: 'virtual-1', kind: 'VIRTUAL' } })
    if (path === '/contests/duel-1' || path === '/contests/virtual-1') return json({ ...VIEW, contest: { ...RUNNING, id: path.split('/')[2] } })
    if (path === '/me/prescription') return json({ date: '', items: [], streak: { days: 0, activeToday: false, atRisk: false } })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return calls
}

const axe = async (page: Page) => {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
}

test('로비는 진행 중·예정·끝남으로 나누고 끝난 것은 쪽으로 서버에서 받는다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/contests')
  const running = page.getByRole('region', { name: /진행 중/ })
  await expect(running.getByRole('link', { name: /주간 대회 41/ })).toBeVisible()
  await expect(running.getByText('끝까지')).toBeVisible()
  await expect(running.getByText(/^1:0\d:\d\d$/)).toBeVisible()
  await expect(page.getByRole('region', { name: /예정/ }).getByText(/1일 \d+시간/)).toBeVisible()

  const ended = page.getByRole('region', { name: /끝남/ })
  await expect(ended.getByRole('link', { name: /^지난 대회 1 / })).toBeVisible()
  await ended.getByRole('button', { name: '2쪽' }).click()
  await expect(ended.getByRole('link', { name: /^지난 대회 21 / })).toBeVisible()
  await expect(page).toHaveURL(/page=2/)
  expect(calls).toContain('GET /contests/finished?page=2')
})

test('받은 코드로 미니 대결에 붙으면 그 대결 화면으로 간다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/contests')
  await page.getByLabel('받은 코드').fill('ab12cd')
  await page.getByRole('button', { name: '코드로 붙기' }).click()
  await expect(page).toHaveURL(/\/contests\/duel-1$/)
  expect(calls).toContain('POST /contests/duels/join')
})

test('진행 중 대회는 남은 시간을 고정 헤더에 세고, 순위표에서 내 줄을 짚는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/contests/c-run')
  const timer = page.getByRole('timer')
  await expect(timer).toHaveAccessibleName(/^끝까지 1:0\d:\d\d$/)
  const first = await timer.getAttribute('aria-label')
  await expect.poll(() => timer.getAttribute('aria-label'), { timeout: 3000 }).not.toBe(first)

  // 내 점수와 프로젝트형 문제의 길
  await expect(page.getByRole('link', { name: '1000. 두 수의 합' })).toHaveAttribute('href', '/problems/two-sum/solve')
  await expect(page.getByRole('link', { name: 'bank-ledger' })).toHaveAttribute('href', '/?project=bank-ledger')
  await expect(page.getByText('60점')).toBeVisible()

  const mine = page.locator('tr[data-mine="true"]')
  await expect(mine).toContainText('시험')
  await expect(mine.getByText('나', { exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: '1:05:30' })).toBeVisible()
  await page.getByRole('button', { name: '내 순위 2위' }).click()
  await expect(mine).toBeFocused()

  // 헤더는 스크롤해도 남는다
  await page.mouse.wheel(0, 2000)
  await expect(timer).toBeInViewport()
})

test('예정 대회는 참가 전에 이름 공개를 미리 말한다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/contests/c-next')
  await expect(page.getByRole('timer')).toHaveAccessibleName(/^시작까지 1일/)
  await expect(page.getByText('참가하면 표시 이름이 이 대회의 순위표에 오릅니다.')).toBeVisible()
  await page.getByRole('button', { name: '참가하기' }).click()
  await expect.poll(() => calls).toContain('POST /contests/c-next/join')
})

test('끝난 대회는 레이팅 변화를 보이고 가상 참가로 다시 돈다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/contests/c-old-1')
  await expect(page.getByRole('timer')).toHaveCount(0)
  await expect(page.getByRole('cell', { name: '-8' })).toBeVisible()
  await expect(page.getByRole('cell', { name: '+12' }).first()).toBeVisible()
  await page.getByRole('button', { name: '가상 참가' }).click()
  await expect(page).toHaveURL(/\/contests\/virtual-1$/)
  expect(calls).toContain('POST /contests/c-old-1/virtual')
})

test('대회 화면들에 접근성 위반이 없고 좁은 화면에서 넘치지 않는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/contests')
  await expect(page.getByRole('link', { name: /^지난 대회 1 / })).toBeVisible()
  expect(await axe(page)).toEqual([])
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)

  await page.goto('/contests/c-run')
  await expect(page.locator('tr[data-mine="true"]')).toBeVisible()
  expect(await axe(page)).toEqual([])
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
})
