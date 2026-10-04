import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 첫 경험 (docs/ui-overhaul.md §6.9) — API 를 흉내 낸다.
 *
 * 닫는 조건: 가입 → 진단 → 첫 처방까지 끊김 없음. 더해서 로그인 전에 쓴 코드가 로그인 뒤로 이어지는지,
 * 비밀번호를 잊은 사람이 돌아오는 길이 있는지.
 */
const PROBLEM = {
  id: 'two-sum',
  number: 1000,
  version: 1,
  title: '두 수의 합',
  statement: '# 두 수의 합\n\n본문',
  timeMillis: 2000,
  memoryMb: 256,
  signature: 'fun twoSum(nums: IntArray, target: Int): IntArray',
  samples: [{ id: '01', args: [[2, 7, 11, 15], 9], expected: [0, 1] }],
  groups: [{ id: 'sample', weight: 0, aggregation: 'ALL_OR_NOTHING', caseCount: 1 }],
}

const SESSION = { accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '새내기', accessExpiresAt: '2099-01-01T00:00:00Z', refreshExpiresAt: '2099-01-01T00:00:00Z' }

interface Options {
  /** 서버에 이미 있는 초안 */
  serverDraft?: string
  onboarded?: boolean
}

async function mockApi(page: Page, options: Options = {}) {
  const calls: { method: string; path: string; body: unknown }[] = []
  let onboarded = options.onboarded ?? false
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    const body = request.postData() ? request.postDataJSON() : null
    calls.push({ method: request.method(), path, body })

    if (path === '/auth/login' || path === '/auth/register') return json(SESSION)
    if (path === '/auth/password/forgot') return route.fulfill({ status: 202 })
    if (path === '/auth/password/reset')
      return (body as { token: string }).token === 'good' ? json({ reset: true }) : json({ errorCode: 'INVALID_SIGNATURE', message: '쓸 수 없는 링크다. 이미 썼거나 시간이 지났다 — 다시 요청한다', traceId: '' }, 400)
    if (path === '/problems/two-sum') return json(PROBLEM)
    if (path === '/problems')
      return json({ items: [{ ...PROBLEM, difficulty: 'EASY', tags: [], competencies: [], solvedRate: null, solvedCount: 0, solved: false }], nextCursor: null, total: 1, tags: {}, page: 1, pageCount: 1 })
    if (path.startsWith('/workspaces/two-sum/')) {
      if (request.method() === 'GET')
        return options.serverDraft === undefined
          ? route.fulfill({ status: 204 })
          : json({ problemId: 'two-sum', language: 'PYTHON', code: options.serverDraft, version: 3, updatedAt: '2026-10-01T00:00:00Z' })
      return json({ problemId: 'two-sum', language: 'PYTHON', code: (body as { code: string }).code, version: 4, updatedAt: '2026-10-04T00:00:00Z' })
    }
    if (path === '/me/onboarding' && request.method() === 'GET')
      return onboarded ? json({ dailyGoal: 2, language: 'PYTHON', level: 'BEGINNER' }) : route.fulfill({ status: 204 })
    if (path === '/me/onboarding' && request.method() === 'PUT') {
      onboarded = true
      return json({
        date: '2026-10-04',
        items: [
          { problemId: 'two-sum', reason: 'DIAGNOSTIC', detail: '진단 — 이해 역량의 첫 근거를 만듭니다. 맞히든 틀리든 기록이 되고, 다음 처방이 그 근거에서 나옵니다.', competency: 'READING', nextMeasurement: '2026-10-04T00:00:00Z' },
        ],
        streak: { days: 0, activeToday: false, atRisk: false },
      })
    }
    if (path === '/me/prescription')
      return json({ date: '2026-10-04', items: [], streak: { days: 0, activeToday: false, atRisk: false } })
    if (path === '/coaching/sessions' || path === '/coaching/transfers') return json([])
    if (path.startsWith('/submissions')) return json({ items: [], nextCursor: null })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return calls
}

const signIn = (page: Page) =>
  page.addInitScript((session) => {
    localStorage.setItem('codedrill.session', JSON.stringify(session))
  }, SESSION)

/**
 * 편집기에 입력한다. 한글은 IME 를 거쳐 Monaco 에서 깨지므로 ASCII 로, 그리고 사람 속도(키 사이 40ms)로 —
 * 간격 0 으로 몰아치면 Monaco 가 키를 흘린다. 사람은 그렇게 치지 않는다.
 */
async function typeInEditor(page: Page, text: string) {
  const editor = page.locator('.monaco-editor .view-lines')
  await expect(editor).toBeVisible({ timeout: 15_000 })
  await editor.click()
  await page.keyboard.type(text, { delay: 40 })
}

const axe = async (page: Page) => {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).exclude('.monaco-editor').analyze()
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
}

test.describe('로그인 전 풀이', () => {
  test.skip(({ isMobile }) => isMobile, '편집기 입력은 데스크톱에서 본다')

  test('로그인 없이 코드를 쓰면 이 기기에 남고, 로그인하면 그대로 이어진다', async ({ page }) => {
    const calls = await mockApi(page)
    await page.goto('/problems/two-sum/solve?lang=PYTHON')
    // 계정의 것(해설·질문·제출)은 없고, 실행·제출 자리에 로그인이 있다
    await expect(page.getByRole('tab')).toHaveText(['문제'])
    await expect(page.getByText('로그인하면 예제로 실행하고 제출할 수 있습니다')).toBeVisible()

    await typeInEditor(page, '# typed before sign-in')
    await expect(page.getByText('이 기기에 저장됨')).toBeVisible()
    expect(await page.evaluate(() => localStorage.getItem('codedrill.local-draft.two-sum.PYTHON'))).toContain('# typed before sign-in')
    // 서버에는 아무것도 보내지 않았다
    expect(calls.filter((c) => c.path.startsWith('/workspaces'))).toEqual([])

    await page.getByRole('button', { name: '로그인하고 실행·제출' }).click()
    await expect(page).toHaveURL(/\/login\?next=%2Fproblems%2Ftwo-sum%2Fsolve%3Flang%3DPYTHON$/)
    await page.getByLabel('이메일').fill('new@example.test')
    await page.getByLabel('비밀번호').fill('long-enough-password')
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    // 서버 초안이 없으니 이어 쓴다 — 자동 저장이 서버로 올리고 기기의 것은 지운다
    await expect(page).toHaveURL(/\/problems\/two-sum\/solve\?lang=PYTHON$/)
    await expect(page.getByText('로그인 전에 쓰던 코드를 이어 씁니다')).toBeVisible()
    await expect
      .poll(() => (calls.find((c) => c.method === 'PUT' && c.path === '/workspaces/two-sum/PYTHON')?.body as { code?: string } | undefined)?.code ?? '', { timeout: 10_000 })
      .toContain('# typed before sign-in')
    expect(await page.evaluate(() => localStorage.getItem('codedrill.local-draft.two-sum.PYTHON'))).toBeNull()
  })

  test('저장된 초안과 다르면 어느 쪽으로 갈지 묻는다', async ({ page }) => {
    await mockApi(page, { serverDraft: '# 서버에 있던 초안' })
    await page.addInitScript(() => localStorage.setItem('codedrill.local-draft.two-sum.PYTHON', '# 기기에 있던 초안'))
    await signIn(page)
    await page.goto('/problems/two-sum/solve?lang=PYTHON')
    const prompt = page.getByRole('status').filter({ hasText: '로그인 전에 이 기기에서 쓴 코드가 있습니다' })
    await expect(prompt).toBeVisible()
    await expect(page.locator('.monaco-editor .view-lines')).toContainText('서버에 있던 초안')
    await prompt.getByRole('button', { name: '그 코드로 이어 쓰기' }).click()
    await expect(page.locator('.monaco-editor .view-lines')).toContainText('기기에 있던 초안')
    await expect(prompt).toHaveCount(0)
  })
})

test('가입하면 세 문항을 묻고, 답하면 진단이 든 첫 처방으로 간다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/login')
  await page.getByRole('button', { name: /가입하기/ }).click()
  await page.getByLabel('이메일').fill('new@example.test')
  await page.getByLabel('비밀번호').fill('long-enough-password')
  await page.getByRole('button', { name: '가입하고 시작' }).click()

  await expect(page).toHaveURL(/\/welcome\?next=%2F$/)
  await expect(page.getByRole('heading', { name: '세 가지만 묻겠습니다' })).toBeVisible()
  await page.getByText('하루 세 문제').click()
  await page.getByText('Kotlin', { exact: true }).click()
  await page.getByText('기본은 한다').click()
  await page.getByRole('button', { name: '진단 시작' }).click()

  await expect(page).toHaveURL(/\/training$/)
  await expect(page.getByText('진단', { exact: true }).first()).toBeVisible()
  await expect(page.getByText('진단 — 이해 역량의 첫 근거를 만듭니다', { exact: false })).toBeVisible()
  expect(calls.find((c) => c.method === 'PUT' && c.path === '/me/onboarding')?.body).toEqual({ dailyGoal: 3, language: 'KOTLIN', level: 'INTERMEDIATE' })
  // 답했으니 다시 권하지 않는다
  await expect(page.getByText('세 가지만 답하면 진단으로 시작합니다')).toHaveCount(0)
})

test('세 문항에 답하지 않았으면 훈련 화면이 진단으로 시작하자고 권한다', async ({ page }) => {
  await mockApi(page)
  await signIn(page)
  await page.goto('/training')
  const prompt = page.getByText('세 가지만 답하면 진단으로 시작합니다')
  await expect(prompt).toBeVisible()
  await expect(page.getByRole('link', { name: '답하기' })).toHaveAttribute('href', '/welcome?next=/training')
})

test('비밀번호 재설정 — 요청의 답은 계정 유무와 무관하고, 링크로 새 비밀번호를 정한다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/login')
  await page.getByRole('link', { name: '비밀번호를 잊었나요?' }).click()
  await expect(page).toHaveURL(/\/forgot-password$/)
  await page.getByLabel('이메일').fill('who@example.test')
  await page.getByRole('button', { name: '재설정 링크 받기' }).click()
  await expect(page.getByText('who@example.test 로 가입했다면 재설정 링크를 보냈습니다', { exact: false })).toBeVisible()
  expect(calls.find((c) => c.path === '/auth/password/forgot')?.body).toEqual({ email: 'who@example.test' })

  await page.goto('/reset-password?token=good')
  await page.getByLabel('새 비밀번호').fill('a-brand-new-password')
  await page.getByLabel('한 번 더').fill('a-different-password')
  await expect(page.getByText('위와 같지 않습니다')).toBeVisible()
  await expect(page.getByRole('button', { name: '바꾸기' })).toBeDisabled()
  await page.getByLabel('한 번 더').fill('a-brand-new-password')
  await page.getByRole('button', { name: '바꾸기' }).click()
  await expect(page.getByText('다른 기기에서 열려 있던 로그인도 모두 끊었습니다', { exact: false })).toBeVisible()

  await page.goto('/reset-password?token=used')
  await page.getByLabel('새 비밀번호').fill('a-brand-new-password')
  await page.getByLabel('한 번 더').fill('a-brand-new-password')
  await page.getByRole('button', { name: '바꾸기' }).click()
  await expect(page.getByText('쓸 수 없는 링크다', { exact: false })).toBeVisible()
  await expect(page.getByRole('link', { name: '다시 요청' })).toBeVisible()
})

test('첫 경험 화면들에 접근성 위반이 없고 좁은 화면에서 넘치지 않는다', async ({ page }) => {
  await mockApi(page)
  for (const path of ['/login', '/forgot-password', '/reset-password?token=x', '/problems/two-sum/solve']) {
    await page.goto(path)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    expect(await axe(page), path).toEqual([])
    expect(await page.evaluate(() => document.documentElement.scrollWidth), path).toBeLessThanOrEqual(page.viewportSize()!.width)
  }
  await signIn(page)
  await page.goto('/welcome')
  await expect(page.getByRole('heading', { name: '세 가지만 묻겠습니다' })).toBeVisible()
  expect(await axe(page)).toEqual([])
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
})
