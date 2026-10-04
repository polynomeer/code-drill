import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 운영 콘솔 (docs/ui-overhaul.md §6.8) — 운영 API 를 흉내 낸다.
 *
 * 닫는 조건: 운영 다섯 큐(문제 버전 검수, 재채점 승인, 신고·제재·이의, 유사도 신호, 아레나 기부)를
 * curl 없이 처리한다. 2인 원칙은 서버가 지키고, 화면은 자기 것에 대한 단추를 막고 서버의 거절 사유를
 * 그대로 보인다.
 */
const ME = 'op-me-0000-0000-0000-000000000001'
const OTHER = 'op-other-000-0000-0000-000000000002'
const USER = 'user-aaaa-0000-0000-0000-000000000003'
const now = new Date().toISOString()

type Roles = ('CONTENT_EDITOR' | 'REVIEWER' | 'PUBLISHER' | 'JUDGE_OPERATOR' | 'SECURITY_ADMIN')[]

async function mockAdmin(page: Page, roles: Roles | null) {
  const posts: { path: string; body: unknown }[] = []
  let rejudges = [
    { id: 'rj-1', scope: 'problem:two-sum', reason: '케이스 수정', status: 'REQUESTED', requestedBy: OTHER, approvedBy: null as string | null, dryRun: false, targetCount: 12, createdAt: now },
    { id: 'rj-2', scope: 'problem:max-subarray', reason: '내가 요청', status: 'REQUESTED', requestedBy: ME, approvedBy: null, dryRun: true, targetCount: 3, createdAt: now },
    { id: 'rj-3', scope: 'problem:bfs', reason: '다른 검수자가 먼저 봄', status: 'REQUESTED', requestedBy: OTHER, approvedBy: null, dryRun: false, targetCount: 5, createdAt: now },
  ]
  let flags = [
    {
      flag: { id: 'flag-1', problemId: 'two-sum', language: 'PYTHON', submissionId: 's-1', otherSubmissionId: 's-2', userId: USER, otherUserId: OTHER, score: 0.93, status: 'OPEN', createdAt: now },
      source: 'def f(a):\n    return a\n',
      otherSource: 'def f(b):\n    return b\n',
    },
  ]

  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'op', displayName: '운영' }))
  })
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (request.method() !== 'GET') posts.push({ path, body: request.postDataJSON() })

    if (!path.startsWith('/admin')) return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
    if (roles === null) return json({ code: 'FORBIDDEN', message: '이 계정에는 관리자 역할이 없다' }, 403)
    if (path === '/admin/me') return json({ userId: ME, roles })

    if (path === '/admin/problems/versions/pending')
      return json([
        { versionId: 'two-sum@3', problemId: 'two-sum', version: 3, packageDigest: 'pkg', reportDigest: 'sha256:abc123', validatorVersion: '7', registeredBy: OTHER, registeredAt: now, publishedVersion: 2 },
        { versionId: 'bfs@1', problemId: 'bfs', version: 1, packageDigest: 'pkg2', reportDigest: 'sha256:fff', validatorVersion: '7', registeredBy: ME, registeredAt: now, publishedVersion: null },
      ])
    if (path === '/admin/problems/two-sum/publish') return json({ publishedVersionId: 'two-sum@3' })

    if (path === '/admin/rejudges' && request.method() === 'GET') return json(rejudges)
    if (path === '/admin/rejudges' && request.method() === 'POST') return json({ ...rejudges[0], id: 'rj-new' })
    if (path === '/admin/rejudges/rj-1/approve') {
      rejudges = rejudges.map((job) => (job.id === 'rj-1' ? { ...job, status: 'APPROVED', approvedBy: ME } : job))
      return json(rejudges[0])
    }
    if (path === '/admin/rejudges/rj-3/approve') return json({ reason: '이미 결정된 작업이다: APPROVED' }, 409)

    if (path === '/admin/discussions/queue')
      return json([
        {
          report: { id: 'rep-1', postId: 'post-1', reporterId: OTHER, reason: '풀이 노출', status: 'OPEN', createdAt: now },
          post: { id: 'post-1', problemId: 'two-sum', kind: 'ANSWER', authorId: USER, title: null, body: '정답 코드: return [0, 1]', status: 'VISIBLE', createdAt: now },
        },
      ])
    if (path === '/admin/discussions/reports/rep-1/resolve') return json({})
    if (path === '/admin/arena/queue')
      return json({
        pending: [{ id: 'don-1', problemId: 'two-sum', submissionId: 's-9', donorUserId: USER, source: 'def two_sum(n, t):\n    return [0, 0]\n', note: '경계에서 틀림', status: 'PENDING', kind: null, createdAt: now }],
        reports: [],
      })
    if (path === '/admin/arena/donations/don-1/approve') return json({})

    if (path === '/admin/integrity/queue') return json(flags)
    if (path === '/admin/integrity/flags/flag-1/resolve') {
      flags = []
      return json({})
    }

    if (path === '/admin/sanctions/appeals')
      return json([
        { id: 'sn-1', userId: USER, kind: 'MUTE', reason: '풀이 노출 반복', evidence: 'report:rep-0', issuedBy: OTHER, startsAt: now, endsAt: null, liftedAt: null, appeal: '실수였습니다', appealedAt: now, appealResolution: null, appealNote: null, createdAt: now },
      ])
    if (path === '/admin/sanctions/sn-1/appeal/resolve') return json({})
    if (path === '/admin/sanctions' && request.method() === 'POST') return json({})
    if (path.startsWith('/admin/sanctions/users/')) return json([])

    if (path === '/admin/operators') return json([{ userId: ME, role: 'SECURITY_ADMIN', grantedBy: 'bootstrap', grantedAt: now }])
    if (path === '/admin/role-requests') return json([{ id: 'rr-1', userId: USER, role: 'REVIEWER', reason: '검수 인력', status: 'REQUESTED', requestedBy: ME, createdAt: now }])
    if (path === '/admin/audit')
      return json(
        url.searchParams.get('subject') === 'two-sum'
          ? [{ id: 2, action: 'PROBLEM_PUBLISHED', subject: 'two-sum', actor: ME, detail: '{"version":3}', createdAt: now }]
          : [
              { id: 2, action: 'PROBLEM_PUBLISHED', subject: 'two-sum', actor: ME, detail: '{"version":3}', createdAt: now },
              { id: 1, action: 'ADMIN_ACCESS_DENIED', subject: 'GET /api/v1/admin/me', actor: USER, detail: '{"status":403}', createdAt: now },
            ],
      )
    return json({ reason: `흉내 내지 않은 경로: ${path}` }, 404)
  })
  return posts
}

const ALL: Roles = ['CONTENT_EDITOR', 'REVIEWER', 'PUBLISHER', 'JUDGE_OPERATOR', 'SECURITY_ADMIN']

const axe = async (page: Page) => {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).exclude('.monaco-editor').analyze()
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
}

test('운영 역할이 없으면 콘솔 대신 왜 없는지를 말한다', async ({ page }) => {
  await mockAdmin(page, null)
  await page.goto('/admin')
  await expect(page.getByText('운영 역할이 없습니다', { exact: true })).toBeVisible()
})

test('역할이 닿는 큐만 보이고, 대기 수를 단다', async ({ page }) => {
  await mockAdmin(page, ['REVIEWER'])
  await page.goto('/admin')
  const nav = page.getByRole('navigation', { name: '운영 큐' })
  await expect(nav.getByRole('link')).toHaveText([/재채점/, /신고·기부 검수/, /유사도 신호/])
  await expect(nav.getByRole('link', { name: /신고·기부 검수/ })).toContainText('2')
  await expect(page.getByRole('link', { name: /유사도 신호.*1건 대기/ })).toBeVisible()
})

test('문제 버전 — 내가 등록한 것은 공개할 수 없고, 공개는 내 보고서 digest 를 적어서 한다', async ({ page }) => {
  const posts = await mockAdmin(page, ALL)
  await page.goto('/admin/versions')
  const rows = page.getByRole('row')
  await expect(rows.filter({ hasText: 'bfs' }).getByRole('button', { name: '공개' })).toBeDisabled()
  await rows.filter({ hasText: 'two-sum' }).getByRole('button', { name: '공개' }).click()

  const dialog = page.getByRole('dialog', { name: 'two-sum v3 공개' })
  await dialog.getByLabel('내 검증 보고서 digest').fill('sha256:wrong')
  await expect(dialog.getByText('등록된 보고서와 다릅니다', { exact: false })).toBeVisible()
  await dialog.getByLabel('내 검증 보고서 digest').fill('sha256:abc123')
  await expect(dialog.getByText('등록된 보고서와 같습니다')).toBeVisible()
  await dialog.getByRole('button', { name: '공개' }).click()
  await expect(page.getByText('two-sum v3 을(를) 공개했습니다')).toBeVisible()
  expect(posts).toContainEqual({ path: '/admin/problems/two-sum/publish', body: { version: 3, reportDigest: 'sha256:abc123', validatorVersion: '7' } })
})

test('재채점 — 내 요청은 승인할 수 없고, 남의 것은 승인한 뒤 실행이 되돌릴 수 없음을 말한다', async ({ page }) => {
  const posts = await mockAdmin(page, ALL)
  await page.goto('/admin/rejudges')
  const mine = page.getByRole('article', { name: '재채점 problem:max-subarray' })
  await expect(mine.getByRole('button', { name: '승인' })).toBeDisabled()

  const other = page.getByRole('article', { name: '재채점 problem:two-sum' })
  await other.getByRole('button', { name: '승인' }).click()
  await page.getByRole('dialog', { name: '재채점 승인' }).getByRole('button', { name: '승인' }).click()
  await expect(other.getByText('실행 대기')).toBeVisible()
  await other.getByRole('button', { name: '실행' }).click()
  await expect(page.getByRole('dialog', { name: '재채점 실행' }).getByText('대상 제출 12건', { exact: false })).toBeVisible()
  expect(posts.map((p) => p.path)).toContain('/admin/rejudges/rj-1/approve')

  // 요청 양식
  await page.getByRole('dialog', { name: '재채점 실행' }).getByRole('button', { name: '취소' }).click()
  await page.getByLabel('범위').fill('submission:abc')
  await page.getByLabel('사유').first().fill('반례 추가')
  await page.getByRole('button', { name: '요청', exact: true }).click()
  await expect.poll(() => posts.find((p) => p.path === '/admin/rejudges')?.body).toEqual({ scope: 'submission:abc', reason: '반례 추가', dryRun: true })
})

test('신고 — 글을 내리고, 아레나 기부는 결함군을 정해 세운다', async ({ page }) => {
  const posts = await mockAdmin(page, ALL)
  await page.goto('/admin/reports')
  const report = page.getByRole('article', { name: '신고 — 풀이 노출' })
  await expect(report.getByText('정답 코드: return [0, 1]')).toBeVisible()
  await expect(report.getByRole('link', { name: '제재 검토로 →' })).toHaveAttribute('href', new RegExp(`/admin/sanctions\\?user=${USER}&evidence=report:rep-1`))
  await report.getByRole('button', { name: '글 내리기' }).click()
  const dialog = page.getByRole('dialog', { name: '글 내리기' })
  await expect(dialog.getByRole('button', { name: '내리기' })).toBeDisabled()
  await dialog.getByLabel('처리 내용').fill('정답 노출')
  await dialog.getByRole('button', { name: '내리기' }).click()
  await expect.poll(() => posts.find((p) => p.path === '/admin/discussions/reports/rep-1/resolve')?.body).toEqual({ hide: true, resolution: '정답 노출' })

  await page.getByRole('tab', { name: '아레나 기부·신고' }).click()
  await page.getByRole('article', { name: '기부 — two-sum' }).getByRole('button', { name: '과녁으로 세우기' }).click()
  const approve = page.getByRole('dialog', { name: '과녁으로 세우기' })
  await approve.getByLabel('결함군').selectOption('OFF_BY_ONE')
  await approve.getByRole('button', { name: '세우기' }).click()
  await expect.poll(() => posts.find((p) => p.path === '/admin/arena/donations/don-1/approve')?.body).toEqual({ kind: 'OFF_BY_ONE', note: null })
})

test('유사도 — 두 소스를 나란히 보고 확인하면, 보안 관리자는 그 근거를 든 제재 검토로 간다', async ({ page }) => {
  const posts = await mockAdmin(page, ALL)
  await page.goto('/admin/integrity')
  await expect(page.getByText('유사도 93%')).toBeVisible()
  await expect(page.locator('.monaco-diff-editor')).toBeVisible({ timeout: 15_000 })
  await page.getByRole('button', { name: '베낀 것으로 확인' }).click()
  const dialog = page.getByRole('dialog', { name: '베낀 것으로 확인' })
  await expect(dialog.getByText('판정도 계정도 바뀌지 않습니다', { exact: false })).toBeVisible()
  await dialog.getByLabel('메모').fill('변수 이름만 다름')
  await dialog.getByRole('button', { name: '확인' }).click()
  await expect(page).toHaveURL(new RegExp(`/admin/sanctions\\?user=${USER}&evidence=similarity:flag-1`))
  await expect(page.getByLabel('근거')).toHaveValue('similarity:flag-1')
  await expect(page.getByLabel('대상 사용자 id')).toHaveValue(USER)
  expect(posts).toContainEqual({ path: '/admin/integrity/flags/flag-1/resolve', body: { confirmed: true, note: '변수 이름만 다름' } })
})

test('제재 — 이의를 판단하고, 근거 없는 발부는 단추가 열리지 않는다', async ({ page }) => {
  const posts = await mockAdmin(page, ALL)
  await page.goto('/admin/sanctions')
  const appeal = page.getByRole('article', { name: '이의 — 풀이 노출 반복' })
  await expect(appeal.getByText('실수였습니다')).toBeVisible()
  await appeal.getByRole('button', { name: '받아들여 해제' }).click()
  const dialog = page.getByRole('dialog', { name: '이의를 받아들여 해제' })
  await dialog.getByLabel('당사자에게 보일 메모').fill('첫 위반이라 해제')
  await dialog.getByRole('button', { name: '해제' }).click()
  await expect.poll(() => posts.find((p) => p.path === '/admin/sanctions/sn-1/appeal/resolve')?.body).toEqual({ uphold: false, note: '첫 위반이라 해제' })

  await page.getByLabel('대상 사용자 id').fill(USER)
  await page.getByLabel('사유', { exact: true }).fill('반복 위반')
  await expect(page.getByRole('button', { name: '발부' })).toBeDisabled()
  await page.getByLabel('근거').fill('report:rep-1')
  await expect(page.getByRole('button', { name: '발부' })).toBeEnabled()
})

test('서버가 거절하면 그 사유를 대화상자에 그대로 보인다', async ({ page }) => {
  await mockAdmin(page, ['REVIEWER', 'SECURITY_ADMIN'])
  await page.goto('/admin/operators')
  const request = page.getByRole('article', { name: '역할 요청 — 검수' })
  // 내가 요청한 것이라 승인 단추가 막혀 있다
  await expect(request.getByRole('button', { name: '승인' })).toBeDisabled()
  await page.goto('/admin/rejudges')
  // 화면이 미리 막지 못하는 거절 — 다른 검수자가 먼저 결정했다. 서버 문장이 대화상자에 그대로 남는다
  await page.getByRole('article', { name: '재채점 problem:bfs' }).getByRole('button', { name: '승인' }).click()
  const dialog = page.getByRole('dialog', { name: '재채점 승인' })
  await dialog.getByRole('button', { name: '승인' }).click()
  await expect(dialog.getByText('이미 결정된 작업이다: APPROVED')).toBeVisible()
})

test('감사 로그는 대상으로 거르고 거부 기록도 보인다', async ({ page }) => {
  await mockAdmin(page, ALL)
  await page.goto('/admin/audit')
  await expect(page.getByRole('cell', { name: '접근 거부' })).toBeVisible()
  await page.getByRole('button', { name: 'two-sum' }).click()
  await expect(page.getByRole('cell', { name: '접근 거부' })).toHaveCount(0)
  await expect(page.getByRole('cell', { name: '문제 공개' })).toBeVisible()
})

test('운영 콘솔에 접근성 위반이 없고 좁은 화면에서 넘치지 않는다', async ({ page }) => {
  await mockAdmin(page, ALL)
  for (const queue of ['', 'versions', 'rejudges', 'reports', 'sanctions', 'operators', 'audit']) {
    await page.goto(`/admin/${queue}`)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.locator('[aria-busy="true"]')).toHaveCount(0)
    expect(await axe(page), queue).toEqual([])
    // innerWidth 가 아니라 기기 폭과 견준다 — 모바일은 넘친 만큼 innerWidth 도 커진다 (design.e2e.ts)
    expect(await page.evaluate(() => document.documentElement.scrollWidth), queue).toBeLessThanOrEqual(page.viewportSize()!.width)
  }
})
