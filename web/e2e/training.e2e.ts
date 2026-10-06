import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 훈련·역량 (docs/ui-overhaul.md §6.5) — API 를 흉내 낸다.
 *
 * 닫는 조건: 처방의 시작·교체·미루기, Evidence drawer 에서 원본 제출 열기.
 */
const SUMMARY = (id: string, number: number, title: string) => ({
  id,
  number,
  version: 1,
  title,
  difficulty: 'EASY',
  tags: [],
  competencies: [],
  solvedRate: null,
  solvedCount: 0,
  solved: false,
})

const PROBLEMS = [
  SUMMARY('two-sum', 1000, '두 수의 합'),
  SUMMARY('max-subarray', 1001, '최대 부분합'),
  SUMMARY('merge-intervals', 1002, '구간 합치기'),
  SUMMARY('valid-brackets', 1003, '올바른 괄호'),
]

const item = (problemId: string, reason: string, detail: string, competency: string | null) => ({
  problemId,
  reason,
  detail,
  competency,
  nextMeasurement: '2026-10-10T00:00:00Z',
})

const STREAK = { days: 3, activeToday: false, atRisk: true }

const MASTERY = [
  { competency: 'READING', group: 'UNDERSTANDING', level: 'PROFICIENT', confidence: 'HIGH', evidenceCount: 8, successCount: 7 },
  { competency: 'CONSTRAINTS', group: 'UNDERSTANDING', level: 'UNMEASURED', confidence: 'NONE', evidenceCount: 0, successCount: 0 },
  { competency: 'MODELING', group: 'DESIGN', level: 'DEVELOPING', confidence: 'LOW', evidenceCount: 2, successCount: 1 },
  { competency: 'ALGORITHM_CHOICE', group: 'DESIGN', level: 'UNMEASURED', confidence: 'NONE', evidenceCount: 0, successCount: 0 },
  { competency: 'EDGE_CASES', group: 'VERIFICATION', level: 'UNMEASURED', confidence: 'NONE', evidenceCount: 0, successCount: 0 },
]

const EVIDENCE = [
  { source: 'SUBMISSION', success: false, weight: 0.5, problemId: 'two-sum', reference: 'sub-1', detail: '숨은 그룹에서 틀림', occurredAt: '2026-10-02T10:00:00Z' },
  { source: 'PREQUESTION', success: true, weight: 1, problemId: 'max-subarray', reference: null, detail: null, occurredAt: '2026-10-03T10:00:00Z' },
]

interface Calls {
  posts: string[]
}

async function mockApi(page: Page, options: { diagnosed?: boolean } = {}): Promise<Calls> {
  const calls: Calls = { posts: [] }
  let items = [
    item('two-sum', 'RECENT_FAILURE', '어제 숨은 그룹에서 틀렸습니다. 같은 실수가 굳기 전에 다시.', 'EDGE_CASES'),
    item('max-subarray', 'WEAK_COMPETENCY', '모델링 근거가 적습니다.', 'MODELING'),
    item('merge-intervals', 'REVIEW_DUE', '복습할 때가 됐습니다.', null),
  ]
  let transfer = { id: 't1', sourceProblemId: 'two-sum', targetProblemId: 'valid-brackets', explanation: null as string | null, status: 'ASSIGNED' }
  let sessions = [
    { id: 's1', problemId: 'merge-intervals', focus: [{ competency: 'MODELING', remaining: 2 }], revealed: [], helpLevel: 1, closed: false },
  ]

  await page.addInitScript(() => {
    localStorage.setItem(
      'codedrill.session',
      JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }),
    )
  })
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (request.method() === 'POST') calls.posts.push(path)

    if (path === '/problems') return json({ items: PROBLEMS, nextCursor: null, total: PROBLEMS.length, tags: {}, page: 1, pageCount: 1 })
    if (path === '/me/prescription') return json({ date: '2026-10-04', items, streak: STREAK })
    const adjust = /^\/me\/prescription\/([^/]+)\/(skip|defer)$/.exec(path)
    if (adjust) {
      items = items.filter((i) => i.problemId !== adjust[1])
      items.push(item('valid-brackets', 'NEXT_ON_PATH', '다음 단계로 넘어갈 때입니다.', null))
      return json({ date: '2026-10-04', items, streak: STREAK })
    }
    if (path === '/coaching/sessions' && request.method() === 'GET') return json(sessions)
    if (path === '/coaching/sessions/s1/close') {
      sessions = []
      return route.fulfill({ status: 204 })
    }
    if (path === '/coaching/transfers') return json(transfer.status === 'VERIFIED' ? [] : [transfer])
    if (path === '/coaching/transfers/t1/explain') {
      transfer = { ...transfer, explanation: 'x', status: 'EXPLAINED' }
      return json(transfer)
    }
    if (path === '/me/competencies')
      return json(options.diagnosed === false ? { diagnosed: false, competencies: MASTERY.map((m) => ({ ...m, level: 'UNMEASURED', confidence: 'NONE', evidenceCount: 0, successCount: 0 })) } : { diagnosed: true, competencies: MASTERY })
    if (path === '/me/competencies/MODELING') return json(EVIDENCE)
    if (path === '/me/report/weekly')
      return json({
        from: '2026-09-27',
        to: '2026-10-04',
        activity: { attempts: 4, accepted: 1, problemsSolved: 1, activeDays: 2 },
        weakest: ['MODELING'],
        recurrences: [],
        growth: [],
        actions: [item('two-sum', 'RECENT_FAILURE', '', null)],
        nextMeasurement: '2026-10-11T00:00:00Z',
      })
    if (path === '/me/stats') return json({ problemsAttempted: 2, problemsSolved: 1, submissions: 4, accepted: 1, verdicts: {}, byTag: {} })
    if (path === '/submissions/sub-1')
      return json({ id: 'sub-1', problemId: 'two-sum', problemVersion: 1, language: 'PYTHON', status: 'COMPLETED', verdict: 'WRONG_ANSWER', score: 0, groups: null, compileLog: null, revision: 1, createdAt: '2026-10-02T10:00:00Z', mine: true })
    if (path.startsWith('/submissions/sub-1/')) return route.fulfill({ status: 204 })
    if (path === '/problems/two-sum')
      return json({ id: 'two-sum', number: 1000, version: 1, title: '두 수의 합', statement: '# 두 수의 합', timeMillis: 1000, memoryMb: 256, signature: 'fun twoSum(nums: IntArray, target: Int): IntArray', samples: [], groups: [] })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return calls
}

const axe = async (page: Page) => {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
}

test.describe('훈련', () => {
  test('처방 카드 둘을 크게, 이유를 문제보다 먼저 보이고 시작은 풀이 화면으로 간다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/training')
    const cards = page.getByRole('article')
    await expect(cards.filter({ hasText: '최근에 틀린 문제' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '1000. 두 수의 합' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '1001. 최대 부분합' })).toBeVisible()
    // 셋째는 후보 목록으로 접힌다
    await expect(page.getByRole('heading', { name: '1002. 구간 합치기' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: '1002. 구간 합치기' })).toBeVisible()
    await expect(page.getByText('오늘 안 하면 끊깁니다')).toBeVisible()

    const first = cards.filter({ hasText: '1000. 두 수의 합' })
    await expect(first.getByRole('link', { name: '시작' })).toHaveAttribute('href', '/problems/two-sum/solve')
  })

  test('교체와 미루기는 각자의 API 를 부르고 빈자리를 다음 후보로 채운다', async ({ page }) => {
    const calls = await mockApi(page)
    await page.goto('/training')
    await page.getByRole('button', { name: /1000\. 두 수의 합 교체/ }).click()
    await expect(page.getByText('1000. 두 수의 합 대신 다른 문제를 골랐습니다')).toBeVisible()
    await expect(page.getByRole('heading', { name: '1000. 두 수의 합' })).toHaveCount(0)

    await page.getByRole('button', { name: /1001\. 최대 부분합 미루기/ }).click()
    await expect(page.getByText('사흘 뒤에 다시 권합니다')).toBeVisible()
    expect(calls.posts).toEqual(['/me/prescription/two-sum/skip', '/me/prescription/max-subarray/defer'])
  })

  test('하던 것 — 전이 과제는 설명을 받고 나서 변형 문제로, 코칭은 이어 하거나 끝낸다', async ({ page }) => {
    const calls = await mockApi(page)
    await page.goto('/training')
    const transfer = page.getByRole('article', { name: /전이 확인/ })
    await transfer.getByLabel(/무엇을 알게 됐나요/).fill('합을 저장해 두면 다시 훑지 않아도 된다')
    await transfer.getByRole('button', { name: '설명 내기' }).click()
    await expect(transfer.getByRole('link', { name: '변형 문제 풀기' })).toHaveAttribute('href', '/problems/valid-brackets/solve')

    const session = page.getByRole('article', { name: /코칭 — 1002\. 구간 합치기/ })
    await expect(session.getByText('1단계까지 봤습니다', { exact: false })).toBeVisible()
    await expect(session.getByRole('link', { name: '이어 하기' })).toHaveAttribute('href', '/problems/merge-intervals/solve?coaching=1')
    await session.getByRole('button', { name: '끝내기' }).click()
    await expect(session).toHaveCount(0)
    expect(calls.posts).toContain('/coaching/sessions/s1/close')
  })

  test('훈련 화면에 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/training')
    await expect(page.getByRole('heading', { name: '1000. 두 수의 합' })).toBeVisible()
    await expect(page.getByRole('article', { name: /코칭/ })).toBeVisible()
    expect(await axe(page)).toEqual([])
  })
})

test.describe('역량', () => {
  test('잰 것만 Level·Confidence 를 따로 그리고, 재지 않은 것은 한 줄로 접는다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/competencies')
    await expect(page.getByRole('link', { name: '역량', exact: true })).toHaveAttribute('aria-current', 'page')
    await expect(page.getByText('3개 역량은 아직 근거가 없습니다')).toBeVisible()
    const reading = page.getByRole('button', { name: /^문제 독해 —/ })
    await expect(reading).toHaveAccessibleName('문제 독해 — 해낸다, 근거 충분, 근거 8개 중 7개 맞힘')
    // 근거가 적으면 약점이라 하지 않는다
    await expect(page.getByRole('button', { name: /^모델링 —.*추가 확인 필요/ })).toBeVisible()
    // 역량군 요약은 수준별로 센다 — 평균이 아니다
    await expect(page.getByRole('list', { name: '역량군 요약' }).getByText('1/2 측정').first()).toBeVisible()
  })

  test('역량을 고르면 근거 drawer 가 열리고 거기서 원본 제출로 간다', async ({ page, isMobile }) => {
    await mockApi(page)
    await page.goto('/competencies')
    await page.getByRole('button', { name: /^모델링 —/ }).click()
    const drawer = isMobile ? page.getByRole('dialog') : page.getByRole('complementary', { name: '근거' })
    await expect(drawer.getByText('추가 확인 필요').first()).toBeVisible()
    await expect(drawer.getByText('도움 받음 · 무게 0.5')).toBeVisible()
    await expect(drawer.getByRole('link', { name: '1000. 두 수의 합' })).toBeVisible()
    await expect(page).toHaveURL(/c=MODELING/)
    await drawer.getByRole('link', { name: '제출 열기' }).click()
    await expect(page).toHaveURL(/\/submissions\/sub-1$/)
  })

  test('이번 주 탭 — 데이터가 적으면 정답률을 말하지 않고, 흔들리는 역량에서 근거로 간다', async ({ page, isMobile }) => {
    await mockApi(page)
    await page.goto('/competencies?tab=weekly')
    await expect(page.getByRole('tab', { name: '이번 주' })).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByText('데이터가 더 필요합니다', { exact: false })).toBeVisible()
    await page.getByRole('button', { name: '모델링 — 근거 보기' }).click()
    await expect(page.getByRole('tab', { name: '역량 지도' })).toHaveAttribute('aria-selected', 'true')
    const drawer = isMobile ? page.getByRole('dialog') : page.getByRole('complementary', { name: '근거' })
    await expect(drawer.getByRole('link', { name: '제출 열기' })).toBeVisible()
  })

  test('아직 아무것도 재지 않았으면 0점이 아니라 진단 시작을 권한다', async ({ page }) => {
    await mockApi(page, { diagnosed: false })
    await page.goto('/competencies')
    await expect(page.getByText('아직 아무것도 재지 않았습니다')).toBeVisible()
    await expect(page.getByRole('link', { name: /진단 시작/ })).toHaveAttribute('href', '/training')
  })

  test('역량 화면에 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/competencies?c=MODELING')
    await expect(page.getByText('도움 받음 · 무게 0.5')).toBeVisible()
    expect(await axe(page)).toEqual([])
  })
})

test.describe('홈 퀘스트 보드', () => {
  test('처방의 첫 칸이 메인 퀘스트, 나머지가 사이드 퀘스트다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/')
    const board = page.getByRole('region', { name: '오늘의 퀘스트' })
    await expect(board.getByText('메인 퀘스트 · 최근에 틀린 문제')).toBeVisible()
    await expect(board.getByRole('heading', { name: '1000. 두 수의 합' })).toBeVisible()
    await expect(board.getByRole('link', { name: '도전하기' })).toHaveAttribute('href', '/problems/two-sum/solve')
    await expect(board.getByRole('list', { name: '사이드 퀘스트' }).getByRole('link')).toHaveText(['1001. 최대 부분합', '1002. 구간 합치기'])
    // 바꾸기·미루기는 훈련 화면 한 곳에만 있다
    await expect(board.getByRole('button')).toHaveCount(0)
  })

  test('플레이어 카드는 서버가 센 숫자만, 스킬 트리는 잰 것만 별로 그린다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/')
    const player = page.getByRole('region', { name: '시험' })
    await expect(player.getByText('3일 연속 · 오늘 안 하면 끊깁니다')).toBeVisible()
    await expect(player.getByText('이번 주 출석 2 / 7일')).toBeVisible()
    await expect(player.getByText(/레벨|XP/)).toHaveCount(0)

    const skills = page.getByRole('region', { name: '스킬 트리' })
    await expect(skills.getByText('역량 1 / 2 잼 · 1개 추가 확인 필요')).toBeVisible()
    await expect(skills.getByRole('listitem').filter({ hasText: '검증' })).toContainText('근거 부족 — 잠김')
  })

  test('아직 재지 않았으면 스킬 트리는 진단 퀘스트를 권한다', async ({ page }) => {
    await mockApi(page, { diagnosed: false })
    await page.goto('/')
    await expect(page.getByRole('region', { name: '스킬 트리' }).getByRole('link', { name: '진단 퀘스트' })).toHaveAttribute('href', '/training')
  })

  test('홈에 접근성 위반이 없다', async ({ page }) => {
    await mockApi(page)
    await page.goto('/')
    await expect(page.getByRole('link', { name: '도전하기' })).toBeVisible()
    expect(await axe(page)).toEqual([])
  })
})
