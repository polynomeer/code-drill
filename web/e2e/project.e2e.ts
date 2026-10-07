import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 프로젝트형 작업 공간의 "로컬에서 풀기" (feature-roadmap 11단계 이어서 — 1단계). API 를 흉내 낸다.
 *
 * 키트(ZIP)의 내용은 서버가 책임진다(ProjectKitTest — 숨은 테스트가 새지 않는지, 세 언어에서 실제로 도는지).
 * 여기서는 화면이 키트를 받게 하고, 받은 폴더를 다시 올릴 때 키트 파일을 빼는지만 본다.
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

async function mockApi(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
  })
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (path === '/projects/job-queue') return json(VIEW)
    if (path === '/projects/job-queue/draft') return route.fulfill({ status: 204 })
    if (path === '/projects/submissions') return json([])
    if (path === '/projects/job-queue/kit')
      return route.fulfill({ status: 200, contentType: 'application/zip', headers: { 'content-disposition': 'attachment; filename="job-queue-v3.zip"' }, body: 'PK' })
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
}

test.skip(({ isMobile }) => isMobile, '작업 공간은 넓은 화면 기준')

test('로컬에서 풀기 — 키트를 받고, 언어에 맞는 실행 명령을 보인다', async ({ page }) => {
  await mockApi(page)
  await page.goto('/?project=job-queue')
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
  await page.goto('/?project=job-queue')
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
  await expect(page.getByRole('tab', { name: 'tests/MyEdgeTest.kt' })).toBeVisible()
  await expect(page.getByRole('tab', { name: 'CODEDRILL.md' })).toHaveCount(0)
})
