import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 프로젝트형 작업 공간 `/projects/:id` (docs/ui-overhaul.md §6.11) — API 를 흉내 낸다.
 *
 * 키트(ZIP)의 내용은 서버가 책임진다(ProjectKitTest — 숨은 테스트가 새지 않는지, 세 언어에서 실제로 도는지).
 * 여기서는 화면이 키트를 받게 하고, 받은 폴더를 다시 올릴 때 키트 파일을 빼는지, 결과를 공개·숨은·점검으로
 * 가르는지, 채점 중 단계를 보이는지를 본다.
 */
const VIEW = {
  id: 'job-queue',
  version: 3,
  title: '재시도가 있는 작업 큐',
  language: 'KOTLIN',
  difficulty: 'MEDIUM',
  tags: [],
  statement: '# 재시도가 있는 작업 큐',
  files: { 'src/queue/JobQueue.kt': 'class JobQueue // TODO', 'tests/PublicJobQueueTest.kt': 'class PublicJobQueueTest' },
  limits: { buildSeconds: 60, testSeconds: 30, memoryMb: 512, maxFiles: 200, maxTotalBytes: 2097152 },
  publicTests: ['tests.PublicJobQueueTest'],
}

const DONE = {
  id: 'ps-1',
  projectId: 'job-queue',
  projectVersion: 3,
  language: 'KOTLIN',
  status: 'COMPLETED',
  verdict: 'WRONG_ANSWER',
  score: 70,
  log: null,
  createdAt: '2026-10-07T10:00:00Z',
  revision: 1,
  tests: [
    { module: 'tests.PublicJobQueueTest', name: 'testSubmitPollAck', passed: true, message: null },
    { module: 'tests.PublicJobQueueTest', name: 'testLeaseExpiresAtDeadline', passed: false, message: 'expected <1> but was <0>' },
    { module: 'tests.MyEdgeTest', name: 'testTokenIsNeverReused', passed: true, message: null },
  ],
  hiddenPassed: 9,
  hiddenTotal: 12,
  probe: { referencePassed: true, killed: ['off-by-one', 'stale-token', 'no-reclaim'], survived: ['token-reuse--a'], log: null, alreadyCaught: ['x', 'y'] },
}

async function mockApi(page: Page, options: { judging?: boolean } = {}) {
  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
  })
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (path === '/projects/job-queue') return json(VIEW)
    if (path === '/projects/job-queue/draft') return route.fulfill({ status: 204 })
    if (path === '/projects/submissions') return json(options.judging ? [] : [DONE])
    if (path === '/projects/submissions/ps-1') return json(DONE)
    if (path === '/projects/job-queue/submissions')
      return json({ ...DONE, id: 'ps-2', status: 'LEASED', verdict: null, score: null, tests: [], hiddenPassed: null, hiddenTotal: null, probe: null })
    if (path === '/projects/submissions/ps-2')
      return json({ ...DONE, id: 'ps-2', status: 'LEASED', verdict: null, score: null, tests: [], hiddenPassed: null, hiddenTotal: null, probe: null })
    if (path === '/projects/job-queue/kit')
      return route.fulfill({ status: 200, contentType: 'application/zip', headers: { 'content-disposition': 'attachment; filename="job-queue-v3.zip"' }, body: 'PK' })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

test.skip(({ isMobile }) => isMobile, '작업 공간은 넓은 화면 기준')

test('로컬에서 풀기 — 키트를 받고, 언어에 맞는 실행 명령을 보인다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/projects/job-queue')
  await page.getByRole('button', { name: '로컬에서 풀기' }).click()
  const dialog = page.getByRole('dialog', { name: '로컬에서 풀기' })
  await expect(dialog.getByText('gradle codedrillTest')).toBeVisible()
  await expect(dialog.getByText('숨은 테스트는 키트에 없습니다', { exact: false })).toBeVisible()
  const download = page.waitForRequest('**/api/v1/projects/job-queue/kit')
  await dialog.getByRole('button', { name: 'job-queue-v3.zip 받기' }).click()
  await download
})

test('받은 폴더를 올리면 키트 파일과 빌드 산출물은 빼고 읽는다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/projects/job-queue')
  // 폴더를 고른 것처럼 — 브라우저가 주는 그대로, 맨 위 폴더 이름부터 시작하는 상대 경로를 단다
  await page.getByLabel('폴더 가져오기').evaluate((input: HTMLInputElement) => {
    const paths = [
      'src/queue/JobQueue.kt',
      'tests/MyEdgeTest.kt',
      'CODEDRILL.md',
      'build.gradle.kts',
      'settings.gradle.kts',
      '.codedrill/project.json',
      '.codedrill/harness/CodedrillHarness.kt',
      'build/classes/kotlin/main/queue/JobQueue.class',
    ]
    const transfer = new DataTransfer()
    for (const path of paths) {
      const file = new File(['x'], path.split('/').pop()!)
      Object.defineProperty(file, 'webkitRelativePath', { value: `job-queue/${path}` })
      transfer.items.add(file)
    }
    input.files = transfer.files
    input.dispatchEvent(new Event('change', { bubbles: true }))
  })
  await expect(page.getByText('2개 파일을 가져왔습니다. 키트·빌드 파일 6개는 빼고 읽었습니다.')).toBeVisible()
  const tree = page.getByRole('navigation', { name: '파일' })
  await expect(tree.getByRole('button', { name: 'MyEdgeTest.kt', exact: true })).toBeVisible()
  await expect(tree.getByText('새 파일')).toHaveCount(1)
  await expect(tree.getByRole('button', { name: 'CODEDRILL.md' })).toHaveCount(0)
})

test('예전 주소 /?project= 는 전용 화면으로 옮겨 준다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/?project=job-queue')
  await expect(page).toHaveURL(/\/projects\/job-queue$/)
  await expect(page.getByRole('heading', { name: '재시도가 있는 작업 큐' })).toBeVisible()
})

test('결과 — 공개 테스트는 이름과 사유로, 숨은 테스트는 수로만, 내가 더 쓴 테스트와 점검은 따로', async ({ page }) => {
  await mockApi(page)
  await page.goto('/projects/job-queue')
  await page.getByRole('tab', { name: '내 제출 1' }).click()
  await page.getByRole('button', { name: /틀렸습니다/ }).click()
  const result = page.getByRole('region', { name: '결과' })
  await expect(result.getByText('70점')).toBeVisible()
  await expect(result.getByText('expected <1> but was <0>')).toBeVisible()
  await expect(result.getByRole('heading', { name: '내가 더 쓴 테스트', exact: true })).toBeVisible()
  await expect(result.getByRole('img', { name: '숨은 테스트 12개 중 9개 통과' })).toBeVisible()
  await expect(result.getByText('오답 4개 중 3개')).toBeVisible()
  await expect(result.getByText('놓친 오답: token-reuse')).toBeVisible()
})

test('제출하면 채점 단계를 보이고 제출 단추를 잠근다', async ({ page }) => {
  await mockApi(page, { judging: true })
  await page.goto('/projects/job-queue')
  await page.getByRole('button', { name: '제출' }).click()
  await expect(page.getByRole('list', { name: '채점 단계' }).getByText('빌드와 테스트')).toHaveAttribute('aria-current', 'step')
  await expect(page.getByRole('button', { name: '채점 중…' })).toBeDisabled()
})

test('작업 공간에 접근성 위반이 없다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/projects/job-queue')
  await page.getByRole('tab', { name: '내 제출 1' }).click()
  await page.getByRole('button', { name: /틀렸습니다/ }).click()
  await expect(page.locator('.monaco-editor')).toBeVisible()
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).exclude('.monaco-editor').analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})
