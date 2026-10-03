import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 판정·제출 화면 (docs/ui-overhaul.md §6.3) — API 를 흉내 낸다.
 *
 * 판정 종류마다 1차 정보가 다르다 — 오답은 입력·기댓값·실행값, 컴파일 오류는 줄 이동, 시스템 오류는
 * "코드 탓이 아니다"와 신고 ID. 실행값은 하네스의 전송 형식(`0,0`)으로 오고 화면이 되돌린다.
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
  groups: [
    { id: 'sample', weight: 0, aggregation: 'ALL_OR_NOTHING', caseCount: 1 },
    { id: 'hidden', weight: 100, aggregation: 'ALL_OR_NOTHING', caseCount: 2 },
  ],
}

const base = {
  problemId: 'two-sum',
  problemVersion: 1,
  language: 'PYTHON',
  status: 'COMPLETED',
  compileLog: null,
  revision: 1,
  createdAt: '2026-10-03T10:00:00Z',
  mine: true,
}

const measurements = { wallTimeMillis: 3, peakMemoryBytes: 1000 }

const SUBMISSIONS = {
  wrong: {
    ...base,
    id: 'wrong',
    verdict: 'WRONG_ANSWER',
    score: 0,
    groups: [
      {
        groupId: 'sample',
        verdict: 'WRONG_ANSWER',
        score: 0,
        maxScore: 0,
        cases: [{ caseId: '01', groupId: 'sample', verdict: 'WRONG_ANSWER', measurements, message: null, actual: '0,0' }],
      },
      { groupId: 'hidden', verdict: 'WRONG_ANSWER', score: 0, maxScore: 100, cases: [] },
    ],
  },
  accepted: {
    ...base,
    id: 'accepted',
    createdAt: '2026-10-03T11:00:00Z',
    verdict: 'ACCEPTED',
    score: 100,
    groups: [
      {
        groupId: 'sample',
        verdict: 'ACCEPTED',
        score: 0,
        maxScore: 0,
        cases: [{ caseId: '01', groupId: 'sample', verdict: 'ACCEPTED', measurements, message: null, actual: '0,1' }],
      },
      { groupId: 'hidden', verdict: 'ACCEPTED', score: 100, maxScore: 100, cases: [] },
    ],
  },
  broken: { ...base, id: 'broken', verdict: 'SYSTEM_ERROR', score: null, groups: null },
  compile: {
    ...base,
    id: 'compile',
    language: 'KOTLIN',
    verdict: 'COMPILE_ERROR',
    score: 0,
    groups: null,
    compileLog: "Solution.kt:3:23: error: unresolved reference 'undefinedThing'.",
  },
  other: { ...base, id: 'other', problemId: 'max-subarray', verdict: 'ACCEPTED', score: 100, groups: null },
} as const

async function mockApi(page: Page, requests: URL[] = []) {
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

    if (path === '/problems/two-sum') return json(PROBLEM)
    if (path === '/problems')
      return json({ items: [{ ...PROBLEM, difficulty: 'EASY', tags: [], competencies: [], solvedRate: null, solvedCount: 0, solved: false }], nextCursor: null, total: 1, tags: {}, page: 1, pageCount: 1 })
    if (path === '/submissions' && route.request().method() === 'GET') {
      requests.push(url)
      const verdict = url.searchParams.get('verdict')
      const items = Object.values(SUBMISSIONS).filter((s) => !verdict || s.verdict === verdict)
      return json({ items, nextCursor: null })
    }
    const match = /^\/submissions\/([^/]+)(\/source|\/trace|\/events)?$/.exec(path)
    if (match) {
      const submission = SUBMISSIONS[match[1] as keyof typeof SUBMISSIONS]
      if (!submission) return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
      if (match[2] === '/source') return json({ source: `# ${submission.id}\nprint(1)\n` })
      if (match[2] === '/trace') return route.fulfill({ status: 204 })
      if (match[2] === '/events') return route.fulfill({ status: 404 })
      return json(submission)
    }
    if (path === '/me/prescription') return json({ date: '2026-10-03', items: [], streak: { days: 0, activeToday: false, atRisk: false } })
    if (path.startsWith('/workspaces/')) return route.fulfill({ status: 204 })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

test('오답은 공개 예제의 입력·기댓값·실행값을 견주고 다른 곳을 짚는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/wrong')
  const verdict = page.getByRole('region', { name: '판정' }).first()
  await expect(verdict.getByText('예제 01 에서 틀렸습니다')).toBeVisible()
  await expect(verdict.getByText('[2,7,11,15], 9')).toBeVisible()
  // 전송 형식 `0,0` 을 [0,0] 으로 되돌려 보인다
  await expect(verdict.locator('mark')).toHaveText(['1]', '0]'])
  await expect(verdict.getByText('4번째 글자부터 다릅니다')).toBeVisible()
  await expect(page.getByRole('heading', { name: '제출한 코드' })).toBeVisible()
})

test('시스템 오류는 코드 탓이 아님을 말하고 신고 ID 를 준다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/broken')
  await expect(page.getByText('플랫폼 오류로 채점하지 못했습니다')).toBeVisible()
  await expect(page.getByText('코드 문제가 아닙니다', { exact: false })).toBeVisible()
  await expect(page.locator('code', { hasText: 'broken' })).toBeVisible()
})

test('맞았으면 다음에 할 것을 보인다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/accepted')
  await expect(page.getByText('다음에 할 것')).toBeVisible()
  await expect(page.getByRole('button', { name: '해설과 다른 풀이' })).toBeVisible()
})

test('컴파일 오류는 풀이 화면에서 그 줄로 가는 버튼이 된다', async ({ page, isMobile }) => {
  await mockApi(page)
  await page.goto('/problems/two-sum/solve?submission=compile')
  if (isMobile) await page.getByRole('tab', { name: '결과' }).click()
  await expect(page.getByRole('button', { name: '3번 줄 23열로 이동' })).toBeVisible()
  await expect(page.getByText("unresolved reference 'undefinedThing'.").first()).toBeVisible()
})

test('제출 목록은 판정으로 거르는 것을 서버에 묻고, 같은 문제의 두 제출을 비교로 넘긴다', async ({ page }) => {
  const requests: URL[] = []
  await mockApi(page, requests)
  await page.goto('/submissions')
  await expect(page.getByRole('link', { name: '1000. 두 수의 합' }).first()).toBeVisible()

  await page.getByLabel('판정').selectOption('ACCEPTED')
  await expect.poll(() => requests.at(-1)?.searchParams.get('verdict')).toBe('ACCEPTED')
  await expect(page).toHaveURL(/verdict=ACCEPTED/)

  await page.getByLabel('판정').selectOption('')
  const compare = page.getByRole('button', { name: /두 제출 비교/ })
  await expect(compare).toBeDisabled()
  const boxes = page.getByRole('checkbox', { name: /두 수의 합/ })
  await boxes.nth(0).check()
  await boxes.nth(1).check()
  await expect(compare).toBeEnabled()
  await compare.click()
  await expect(page).toHaveURL(/\/submissions\/compare\?a=.+&b=.+/)
})

test('다른 문제의 제출은 견주지 않는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/compare?a=wrong&b=other')
  await expect(page.getByText('다른 문제의 제출은 견주지 않습니다')).toBeVisible()
})

test('제출 상세에 접근성 위반이 없다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/wrong')
  await expect(page.getByText('예제 01 에서 틀렸습니다')).toBeVisible()
  const result = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .exclude('.monaco-editor')
    .analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})
