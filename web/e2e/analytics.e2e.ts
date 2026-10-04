import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 계측 게이트 (docs/ui-overhaul.md §8, 디자인 설계서 §16.1·§16.2) — API 를 흉내 낸다.
 *
 * 핵심 흐름(목록 → 문제 → 실행 → 제출 → 판정 → 리플레이 → 걸음 옮기기 → 코드 줄)을 한 번 지나며 §16.1 의
 * 이벤트가 **빠짐없이, 한 번씩** 오는지, 그리고 어떤 이벤트에도 소스·입력·출력이 실리지 않는지 본다.
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

const SOURCE_MARKER = 'SECRET_SOURCE_LINE'

const EVENTS = [1, 2, 3, 4].map((seq) => ({
  seq,
  logicalTime: seq,
  eventType: seq === 4 ? 'MATCH' : 'COMPARE',
  targetKind: 'ARRAY',
  targetRef: String(seq - 1),
  before: null,
  after: seq === 4 ? '1' : null,
  importance: seq === 4 ? 3 : 2,
  sourceLine: seq + 1,
  attributes: {},
}))

const SUBMISSION = {
  id: 's1',
  problemId: 'two-sum',
  problemVersion: 1,
  language: 'PYTHON',
  status: 'COMPLETED',
  verdict: 'WRONG_ANSWER',
  score: 0,
  groups: null,
  compileLog: null,
  revision: 1,
  createdAt: '2026-10-05T00:00:00Z',
  mine: true,
}

async function mockApi(page: Page) {
  const received: { name: string; props: Record<string, unknown> }[] = []
  const bodies: string[] = []
  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
  })
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

    if (path === '/events') {
      const raw = request.postData() ?? ''
      bodies.push(raw)
      received.push(...(JSON.parse(raw) as { events: typeof received }).events)
      return json({ accepted: 1, dropped: 0 }, 202)
    }
    if (path === '/problems')
      return json({ items: [{ ...PROBLEM, difficulty: 'EASY', tags: ['hash'], competencies: [], solvedRate: null, solvedCount: 3, solved: false }], nextCursor: null, total: 1, tags: { hash: 1 }, page: 1, pageCount: 1 })
    if (path === '/problems/two-sum') return json(PROBLEM)
    if (path.startsWith('/workspaces/')) return route.fulfill({ status: 204 })
    if (path === '/me/onboarding') return route.fulfill({ status: 204 })
    if (path === '/trials' && request.method() === 'POST') return json({ id: 'tr1', problemId: 'two-sum', language: 'PYTHON', status: 'COMPLETED', compileLog: null, cases: [] })
    if (path === '/trials/tr1') return json({ id: 'tr1', problemId: 'two-sum', language: 'PYTHON', status: 'COMPLETED', compileLog: null, cases: [] })
    if (path === '/submissions' && request.method() === 'POST') return json({ ...SUBMISSION, status: 'QUEUED', verdict: null })
    if (path === '/submissions' && request.method() === 'GET') return json({ items: [], nextCursor: null })
    if (path === '/submissions/s1') return json(SUBMISSION)
    if (path === '/submissions/s1/source') return json({ source: `# ${SOURCE_MARKER}\nprint(1)\nprint(2)\nprint(3)\nprint(4)\nprint(5)\n` })
    if (path === '/submissions/s1/trace')
      return json({ schemaVersion: '2.0', traceId: 't1', submissionId: 's1', caseId: '01', status: 'READY', eventCount: 4, chunks: [{ index: 0, firstSeq: 1, lastSeq: 4, eventCount: 4 }], summary: EVENTS, truncated: false, diagnostics: null })
    if (path === '/submissions/s1/trace/chunks/0') return json({ schemaVersion: '2.0', traceId: 't1', index: 0, events: EVENTS })
    if (path === '/submissions/s1/events') return route.fulfill({ status: 404 })
    if (path === '/submissions/s1/divergence') return json({ submissionId: 's1', caseId: '01', outcome: 'SAME', sharedPrefix: null, divergedAtSeq: null, sourceLine: null, expectedStep: null, actualStep: null })
    if (path === '/submissions/s1/predictions') return json([])
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return { received, bodies }
}

test('핵심 흐름의 §16.1 이벤트가 빠짐없이 한 번씩 오고, 소스는 실리지 않는다', async ({ page, isMobile }) => {
  test.skip(isMobile, '흐름은 데스크톱에서 본다')
  const { received, bodies } = await mockApi(page)

  // 목록 → 문제
  await page.goto('/problems')
  await page.getByRole('link', { name: '두 수의 합' }).click()
  await expect(page).toHaveURL(/\/problems\/two-sum\/solve/)
  await expect(page.locator('.monaco-editor .view-lines')).toBeVisible({ timeout: 15_000 })

  // 실행(예제 그대로) → 제출 → 판정
  await page.getByRole('button', { name: '실행' }).first().click()
  await page.getByRole('button', { name: '제출', exact: true }).click()
  await expect(page.getByText('예제', { exact: false }).first()).toBeVisible()

  // 모아 보내기(2초)가 끝날 때까지 — 페이지를 떠날 때의 keepalive 전송은 Playwright 가 가로채지 못한다
  await expect.poll(() => received.length, { timeout: 10_000 }).toBe(5)

  // 리플레이 전용 화면 → 키로 한 걸음 → 코드 줄로
  await page.goto('/submissions/s1/replay?step=1')
  await expect(page.locator('li[aria-current="step"]')).toBeVisible()
  await page.locator('body').press('ArrowRight')
  await page.getByRole('button', { name: '5번 줄의 다음 걸음으로' }).click()

  await expect
    .poll(() => received.map((e) => e.name).sort(), { timeout: 10_000 })
    .toEqual(
      [
        'code_trace_link_used',
        'problem_list_view',
        'problem_open',
        'replay_opened',
        'replay_seeked',
        'replay_seeked',
        'run_requested',
        'submission_created',
        'verdict_viewed',
      ].sort(),
    )

  const one = (name: string) => received.filter((e) => e.name === name)
  expect(one('problem_open')[0]!.props).toEqual({ source: 'list', problemId: 'two-sum' })
  expect(one('run_requested')[0]!.props).toEqual({ type: 'sample', language: 'KOTLIN' })
  expect(one('submission_created')[0]!.props).toEqual({ problem: 'two-sum', language: 'KOTLIN' })
  expect(one('verdict_viewed')[0]!.props.verdict).toBe('WRONG_ANSWER')
  expect(typeof one('verdict_viewed')[0]!.props.latency).toBe('number')
  expect(one('replay_opened')[0]!.props).toEqual({ entry: 'link', traceType: 'READY' })
  expect(one('replay_seeked').map((e) => e.props.method).sort()).toEqual(['key', 'line'])
  expect(one('code_trace_link_used')[0]!.props).toEqual({ direction: 'code-to-state', eventType: 'MATCH' })

  // §16.2 — 소스·입력·출력 전문 금지
  for (const body of bodies) {
    expect(body).not.toContain(SOURCE_MARKER)
    expect(body).not.toContain('raise NotImplementedError')
    expect(body).not.toContain('[2,7,11,15]')
    // 누구인지는 보내지 않는다
    expect(body).not.toContain('u1')
  }
})
