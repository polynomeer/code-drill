import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 프로필 (docs/ui-overhaul.md §6.7) — API 를 흉내 낸다.
 *
 * 닫는 조건: 공개 프로필 히트맵. 더해서 공개 범위 — 비공개는 남에게 없는 것이고, 역량 수준은 어디에도
 * 실리지 않는다.
 */
function activity() {
  const today = new Date()
  return Array.from({ length: 365 }, (_, i) => {
    const day = new Date(today.getTime() - (364 - i) * 86400_000)
    return { date: day.toISOString().slice(0, 10), submissions: i > 360 ? 12 : i % 7 === 0 ? 3 : 0 }
  })
}

const PROFILE = {
  handle: 'ada',
  displayName: '에이다',
  joinedAt: '2026-01-10T00:00:00Z',
  mine: false,
  public: true,
  solved: { total: 5, byDifficulty: { EASY: 3, HARD: 2 } },
  activity: activity(),
  streak: { current: 4, longest: 9 },
  rating: {
    rating: 1532,
    contests: 2,
    history: [
      { contestId: 'c2', title: '주간 대회 2', rank: 1, before: 1490, after: 1532, at: '2026-09-20T00:00:00Z' },
      { contestId: 'c1', title: '주간 대회 1', rank: 4, before: 1500, after: 1490, at: '2026-09-01T00:00:00Z' },
    ],
  },
  solutions: [{ postId: 'p1', problemId: 'two-sum', title: '해시맵 한 번 훑기', helpful: 3, at: '2026-09-30T00:00:00Z' }],
  contributorTier: 'ACTIVE',
}

async function mockApi(page: Page, options: { signedIn?: boolean; mine?: boolean; handle?: string | null } = {}) {
  const puts: unknown[] = []
  let settings = { handle: options.handle === undefined ? 'ada' : options.handle, public: false }
  if (options.signedIn) {
    await page.addInitScript(() => {
      localStorage.setItem(
        'codedrill.session',
        JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '에이다' }),
      )
    })
  }
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

    if (path === '/problems')
      return json({ items: [{ id: 'two-sum', number: 1000, version: 1, title: '두 수의 합', difficulty: 'EASY', tags: [], competencies: [], solvedRate: null, solvedCount: 0, solved: false }], nextCursor: null, total: 1, tags: {}, page: 1, pageCount: 1 })
    if (path === '/profiles/ada' || path === `/profiles/${settings.handle}`)
      return json({ ...PROFILE, handle: settings.handle ?? 'ada', mine: !!options.mine, public: options.mine ? settings.public : true })
    if (path.startsWith('/profiles/')) return route.fulfill({ status: 404 })
    if (path === '/auth/me/profile' && request.method() === 'GET') return json(settings)
    if (path === '/auth/me/profile' && request.method() === 'PUT') {
      const body = request.postDataJSON() as { handle: string; public: boolean }
      puts.push(body)
      settings = { handle: body.handle, public: body.public }
      return json(settings)
    }
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return puts
}

test('공개 프로필은 로그인 없이 열리고, 1년 히트맵과 레이팅·풀이를 보이되 역량 수준은 없다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/u/ada')
  await expect(page.getByRole('heading', { level: 1, name: '에이다' })).toBeVisible()
  await expect(page.getByText('@ada · 2026년 1월 가입')).toBeVisible()

  const heatmap = page.getByRole('img', { name: /최근 1년 제출 \d+회, 활동한 날 \d+일/ })
  await expect(heatmap).toBeVisible()
  await expect(heatmap.locator('rect')).toHaveCount(365)
  // 고정 문턱 — 하루 12번은 가장 진한 칸
  await expect(heatmap.locator('rect').last().locator('title')).toHaveText(/제출 12회/)

  await expect(page.getByRole('img', { name: '레이팅 1500에서 1532, 대회 2회' })).toBeVisible()
  await expect(page.getByRole('cell', { name: '+42' })).toBeVisible()
  await expect(page.getByRole('link', { name: '해시맵 한 번 훑기' })).toHaveAttribute('href', '/problems/two-sum')
  await expect(page.getByText('기여자', { exact: true })).toBeVisible()

  // 역량 수준과 본인 전용 행동은 남에게 없다
  for (const word of ['기르는 중', '해낸다', '단단하다', '공개 설정', '내 역량 지도']) {
    await expect(page.getByText(word, { exact: false })).toHaveCount(0)
  }
})

test('없거나 비공개인 핸들은 같은 빈 화면이다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/u/ghost')
  await expect(page.getByText('프로필이 없거나 공개되지 않았습니다')).toBeVisible()
})

test('본인은 비공개여도 미리 보고, 공개 범위를 읽은 뒤 켠다', async ({ page }) => {
  const puts = await mockApi(page, { signedIn: true, mine: true })
  await page.goto('/u/ada')
  await expect(page.getByText('비공개 — 나만 봄')).toBeVisible()
  await expect(page.getByRole('link', { name: '내 역량 지도' })).toHaveAttribute('href', '/competencies')

  await page.getByRole('button', { name: '공개 설정' }).click()
  const dialog = page.getByRole('dialog', { name: '공개 프로필 설정' })
  const check = dialog.getByRole('checkbox', { name: /프로필을 공개합니다/ })
  await expect(check).not.toBeChecked()
  await expect(dialog.getByText('역량 수준과 코드·제출 내용은 보이지 않습니다', { exact: false })).toBeVisible()
  await check.check()
  await dialog.getByRole('button', { name: '저장' }).click()
  await expect(page.getByText('공개', { exact: true })).toBeVisible()
  expect(puts).toEqual([{ handle: 'ada', public: true }])
})

test('/u/me 는 핸들이 없으면 그 자리에서 정하고 내 프로필로 간다', async ({ page }) => {
  const puts = await mockApi(page, { signedIn: true, mine: true, handle: null })
  await page.goto('/u/me')
  await expect(page.getByRole('heading', { name: '내 프로필' })).toBeVisible()
  // 핸들 없이는 공개를 켤 수 없다
  await expect(page.getByRole('checkbox', { name: /프로필을 공개합니다/ })).toBeDisabled()
  await page.getByLabel('핸들').fill('new-ada')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page).toHaveURL(/\/u\/new-ada$/)
  expect(puts).toEqual([{ handle: 'new-ada', public: false }])
})

test('사용자 메뉴의 내 프로필은 내 핸들 주소로 간다', async ({ page }) => {
  await mockApi(page, { signedIn: true, mine: true })
  await page.goto('/problems')
  await page.getByRole('button', { name: /계정 메뉴/ }).click()
  await page.getByRole('link', { name: '내 프로필' }).click()
  await expect(page).toHaveURL(/\/u\/ada$/)
})

test('프로필에 접근성 위반이 없고 좁은 화면에서 넘치지 않는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/u/ada')
  await expect(page.getByRole('img', { name: /최근 1년/ })).toBeVisible()
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(0)
})
