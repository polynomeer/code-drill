import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * R-01 실행 리플레이 (docs/ui-overhaul.md §6.4) — API 를 흉내 낸다.
 *
 * 닫는 조건은 UI 디자인 문서 §11.2 Replay — **같은 seq 에서 네 pane 상태 일치**. 걸음을 어떻게
 * 옮기든(주소·키보드·코드 줄·스크러버) 코드의 활성 줄, 캔버스의 현재 칸, 검사기의 문장, 타임라인의
 * 값이 같은 걸음을 가리켜야 한다.
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

const SOURCE = [
  'def two_sum(nums, target):',
  '    # 정렬된 배열의 투 포인터',
  '    left = 0',
  '    right = len(nums) - 1',
  '    while left < right:',
  '        s = nums[left] + nums[right]',
  '        if s == target: return [left, right]',
  '        left += 1',
  '    return []',
].join('\n')

// [seq, type, ref, after, importance, line] — seq N 이 곧 걸음 N 이다
const RAW: [number, string, string, string | null, number, number][] = [
  [1, 'POINTER', '0', 'left', 1, 3],
  [2, 'POINTER', '3', 'right', 1, 4],
  [3, 'COMPARE', '0', null, 2, 6],
  [4, 'COMPARE', '3', null, 2, 6],
  [5, 'POINTER', '1', 'left', 1, 8],
  [6, 'COMPARE', '1', null, 2, 6],
  [7, 'COMPARE', '3', null, 2, 6],
  [8, 'MATCH', '1', '3', 3, 7],
]
const EVENTS = RAW.map(([seq, eventType, targetRef, after, importance, sourceLine]) => ({
  seq,
  logicalTime: seq,
  eventType,
  targetKind: 'ARRAY',
  targetRef,
  before: null,
  after,
  importance,
  sourceLine,
  attributes: {},
}))

const MANIFEST = {
  schemaVersion: '2.0',
  traceId: 't1',
  submissionId: 'r1',
  caseId: '01',
  status: 'READY',
  eventCount: EVENTS.length,
  chunks: [{ index: 0, firstSeq: 1, lastSeq: EVENTS.length, eventCount: EVENTS.length }],
  summary: EVENTS,
  truncated: false,
  diagnostics: null,
}

const SUBMISSION = {
  id: 'r1',
  problemId: 'two-sum',
  problemVersion: 1,
  language: 'PYTHON',
  status: 'COMPLETED',
  verdict: 'WRONG_ANSWER',
  score: 0,
  groups: null,
  compileLog: null,
  revision: 1,
  createdAt: '2026-10-03T10:00:00Z',
  mine: true,
}

const DIVERGENCE = {
  submissionId: 'r1',
  caseId: '01',
  outcome: 'DIVERGED',
  sharedPrefix: 4,
  divergedAtSeq: 5,
  sourceLine: 8,
  expectedStep: 'POINTER [2] → right',
  actualStep: 'POINTER array[1] → left',
}

async function mockApi(page: Page, overrides: { trace?: unknown } = {}) {
  await page.addInitScript(() => {
    localStorage.setItem(
      'codedrill.session',
      JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }),
    )
  })
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

    if (path === '/problems/two-sum') return json(PROBLEM)
    if (path === '/problems')
      return json({ items: [{ ...PROBLEM, difficulty: 'EASY', tags: [], competencies: [], solvedRate: null, solvedCount: 0, solved: false }], nextCursor: null })
    if (path === '/submissions/r1') return json(SUBMISSION)
    if (path === '/submissions/r1/source') return json({ source: SOURCE })
    if (path === '/submissions/r1/trace')
      return 'trace' in overrides ? (overrides.trace === null ? route.fulfill({ status: 204 }) : json(overrides.trace)) : json(MANIFEST)
    if (path === '/submissions/r1/trace/chunks/0') return json({ schemaVersion: '2.0', traceId: 't1', index: 0, events: EVENTS })
    if (path === '/submissions/r1/divergence') return json(DIVERGENCE)
    if (path === '/submissions/r1/predictions') return json([])
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

/** 네 pane 이 가리키는 걸음. 하나라도 다르면 그 pane 의 값이 드러나도록 객체로 견준다. */
async function panes(page: Page) {
  const canvasStep = await page.locator('[data-step]').getAttribute('data-step')
  return {
    timeline: await page.getByRole('slider', { name: '재생 위치' }).inputValue(),
    canvas: canvasStep,
    code: await page.locator('li[aria-current="step"]').getAttribute('data-line').catch(() => null),
    inspector: await page.locator('#inspector-current + div p').first().textContent(),
  }
}

test.describe('데스크톱', () => {
  test.skip(({ isMobile }) => isMobile, '세 pane 은 넓은 화면에서만 나란히 선다')

  test('주소의 걸음에서 네 pane 이 같은 걸음을 가리킨다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/submissions/r1/replay?step=3')
    await expect(page.getByRole('heading', { name: /1000\. 두 수의 합/ })).toBeVisible()
    // 걸음 3 = COMPARE 0, 6번 줄
    await expect.poll(() => panes(page)).toEqual({
      timeline: '3',
      canvas: '3',
      code: '6',
      inspector: '단계 3: 인덱스 0을 비교했습니다',
    })
    await expect(page.getByRole('button', { name: /인덱스 0, 값 2, 현재/ })).toBeVisible()
  })

  test('키보드·코드 줄·스크러버로 옮겨도 네 pane 이 함께 간다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/submissions/r1/replay?step=3')
    await expect.poll(() => panes(page).then((p) => p.timeline)).toBe('3')

    // → 한 걸음: 걸음 4 = COMPARE 3
    await page.locator('body').press('ArrowRight')
    await expect.poll(() => panes(page)).toEqual({
      timeline: '4',
      canvas: '4',
      code: '6',
      inspector: '단계 4: 인덱스 3을 비교했습니다',
    })
    await expect(page).toHaveURL(/step=4/)

    // Shift+→ 다음 중요 이벤트: 걸음 5 는 분기(오류 마커)다
    await page.locator('body').press('Shift+ArrowRight')
    await expect.poll(() => panes(page).then((p) => [p.timeline, p.code])).toEqual(['5', '8'])
    await expect(page.locator('#inspector-current + div').getByText('분기', { exact: true })).toBeVisible()

    // 코드 → 상태: 7번 줄을 누르면 그 줄이 부른 걸음 8 로
    await page.getByRole('button', { name: '7번 줄의 다음 걸음으로' }).click()
    await expect.poll(() => panes(page)).toEqual({
      timeline: '8',
      canvas: '8',
      code: '7',
      inspector: '단계 8: 인덱스 1과 3에서 답을 찾았습니다',
    })

    // 스크러버
    await page.getByRole('slider', { name: '재생 위치' }).fill('2')
    await expect.poll(() => panes(page).then((p) => [p.timeline, p.canvas, p.code])).toEqual(['2', '2', '4'])
  })

  test('캔버스에서 칸을 고르면 그 칸을 건드린 줄과 마지막 걸음을 짚는다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/submissions/r1/replay?step=7')
    await page.getByRole('button', { name: /^인덱스 3, 값 15/ }).click()
    const selection = page.getByRole('region', { name: /고른 칸 3/ })
    await expect(selection.getByRole('button', { name: '단계 7' })).toBeVisible()
    // 칸 3 은 4번 줄(right)과 6번 줄(비교)이 건드렸다
    await expect(page.locator('li[data-line="4"]')).toHaveClass(/lineTouched/)
    await expect(page.locator('li[data-line="6"]')).toHaveClass(/lineTouched/)
    await expect(page.locator('li[data-line="8"]')).not.toHaveClass(/lineTouched/)
  })

  test('분기 카드는 다섯 단계로 말하고 그 걸음과 줄로 데려간다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/submissions/r1/replay?step=0')
    const card = page.getByRole('region', { name: '5번째 걸음에서 참조 풀이와 갈렸습니다' })
    await expect(card.getByText('4걸음까지 같았습니다.')).toBeVisible()
    await expect(card.getByText('포인터 left을(를) 인덱스 1에 두었습니다').first()).toBeVisible()
    await expect(card.getByText('01 · 입력 [2, 7, 11, 15]')).toBeVisible()
    await card.getByRole('button', { name: '갈린 걸음으로 이동' }).click()
    await expect.poll(() => panes(page).then((p) => [p.timeline, p.code])).toEqual(['5', '8'])
    // 타임라인의 오류 마커는 색만이 아니라 글자로도
    await expect(page.getByRole('region', { name: '타임라인' }).getByText('분기')).toBeVisible()
  })

  test('재생은 중요 이벤트 사이를 건너뛰고 끝에서 멈춘다', async ({ page }) => {
    await mockApi(page)
    await page.addInitScript(() => localStorage.setItem('codedrill.replay.speed', '2'))
    await page.goto('/submissions/r1/replay?step=0')
    await page.getByRole('button', { name: '재생 (Space)' }).click()
    await expect(page.getByRole('slider', { name: '재생 위치' })).toHaveValue('8', { timeout: 5000 })
    await expect(page.getByRole('button', { name: '재생 (Space)' })).toBeVisible()
  })

  test('리플레이 화면에 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/submissions/r1/replay?step=5')
    await expect.poll(() => panes(page).then((p) => p.timeline)).toBe('5')
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
    expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
})

test('줄어든 움직임에서는 칸 전환이 즉시 일어난다', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await mockApi(page)
  await page.goto('/submissions/r1/replay?step=3')
  const cell = page.locator('[data-step] .cell').first()
  await expect(cell).toBeVisible()
  const duration = await cell.evaluate((element) => getComputedStyle(element).transitionDuration)
  expect(duration.split(',').every((value) => parseFloat(value) <= 0.0001)).toBe(true)
})

test('좁은 화면에서는 상태가 먼저이고 검사기는 아래 시트다', async ({ page, isMobile }) => {
  test.skip(!isMobile, '모바일 배치')
  await mockApi(page)
  await page.goto('/submissions/r1/replay?step=3')
  await expect(page.getByRole('tab', { name: '상태' })).toHaveAttribute('aria-selected', 'true')
  const sheet = page.locator('details summary')
  await expect(sheet).toContainText('단계 3: 인덱스 0을 비교했습니다')
  await sheet.click()
  await expect(page.getByRole('heading', { name: '현재 이벤트' })).toBeVisible()
  // 화면 밖으로 넘치지 않는다
  // innerWidth 가 아니라 기기 폭과 견준다 — 모바일은 넘친 만큼 innerWidth 도 커진다 (design.e2e.ts)
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(page.viewportSize()!.width)
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})

test('트레이스가 없으면 왜 없는지와 돌아갈 길을 준다', async ({ page }) => {
  await mockApi(page, { trace: null })
  await page.goto('/submissions/r1/replay')
  await expect(page.getByText('이 제출에는 리플레이가 없습니다')).toBeVisible()
  await expect(page.getByRole('link', { name: '제출로 돌아가기' }).last()).toBeVisible()
})

test('제출 상세의 ?step= 링크는 리플레이 화면의 그 걸음으로 간다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/submissions/r1?step=4')
  await expect(page).toHaveURL(/\/submissions\/r1\/replay\?step=4/)
  await expect(page.getByRole('slider', { name: '재생 위치' })).toHaveValue('4')
})
