import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * CLI 로그인 승인 `/device` 와 연결된 기기 (RFC 8628, feature-roadmap 11단계 이어서 2단계) — API 를 흉내 낸다.
 *
 * 만료·폴링·발급의 규칙은 서버가 책임진다(DeviceAuthorizationTest). 여기서는 사람이 코드를 맞춰 보고
 * 승인·거절하는 흐름과, 연결된 기기를 끊는 흐름을 본다.
 */
const REQUEST = {
  userCode: 'WQXR-KDPB',
  deviceName: 'MacBook-Pro (zsh)',
  client: 'codedrill 1.0 · macOS',
  createdAt: '2026-10-07T10:00:00Z',
  expiresAt: '2026-10-07T10:10:00Z',
}

async function mockApi(page: Page, options: { pending?: boolean } = {}) {
  const calls: string[] = []
  let devices = [{ id: 'd1', deviceName: 'work-desktop (PowerShell)', client: 'codedrill 1.0 · Windows', connectedAt: '2026-09-30T09:00:00Z', lastUsedAt: '2026-10-04T09:00:00Z' }]
  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
  })
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (path === '/auth/device/requests/WQXR-KDPB') {
      if (options.pending === false) return route.fulfill({ status: 404 })
      if (request.method() === 'POST') {
        calls.push(`decide ${request.postDataJSON().approve}`)
        return route.fulfill({ status: 204 })
      }
      return json(REQUEST)
    }
    if (path === '/auth/devices') return json(devices)
    if (path === '/auth/devices/d1' && request.method() === 'DELETE') {
      calls.push('disconnect d1')
      devices = []
      return route.fulfill({ status: 204 })
    }
    return json({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }, 404)
  })
  return calls
}

test('링크로 들어오면 코드와 기기를 보이고, 승인하면 터미널로 돌려보낸다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/device?code=wqxrkdpb')
  await expect(page.getByRole('heading', { name: 'CLI 로그인 승인' })).toBeVisible()
  await expect(page.getByLabel('승인 코드 WQXR-KDPB')).toBeVisible()
  await expect(page.getByText('MacBook-Pro (zsh)')).toBeVisible()
  await expect(page.getByText('내가 터미널에서 방금 연 요청이 아니면 승인하지 마세요')).toBeVisible()

  const axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
  expect(axe.violations).toEqual([])

  await page.getByRole('button', { name: '승인' }).click()
  await expect(page.getByText('승인했습니다')).toBeVisible()
  expect(calls).toEqual(['decide true'])
})

test('코드 없이 들어오면 입력을 받고, 거절할 수 있다', async ({ page }) => {
  const calls = await mockApi(page)
  await page.goto('/device')
  const field = page.getByLabel('터미널에 보이는 코드')
  await field.fill('WQXR-KDPA')
  await expect(page.getByRole('button', { name: '확인' })).toBeDisabled()
  await field.fill('wqxr-kdpb')
  await page.getByRole('button', { name: '확인' }).click()
  await page.getByRole('button', { name: '거절' }).click()
  await expect(page.getByText('거절했습니다')).toBeVisible()
  expect(calls).toEqual(['decide false'])
})

test('만료됐거나 이미 결정한 코드면 다시 로그인하라고 한다', async ({ page }) => {
  await mockApi(page, { pending: false })
  await page.goto('/device?code=WQXR-KDPB')
  await expect(page.getByText('기다리는 요청이 없습니다')).toBeVisible()
  await expect(page.getByLabel('터미널에 보이는 코드')).toHaveValue('WQXR-KDPB')
})

test('계정 설정에서 연결된 기기를 끊는다', async ({ page, isMobile }) => {
  test.skip(isMobile, '사용자 메뉴는 넓은 화면 기준 — 좁은 화면은 navigation 의 "나" 경로')
  const calls = await mockApi(page)
  await page.goto('/device')
  await page.getByRole('button', { name: '시험 계정 메뉴' }).click()
  await page.getByRole('button', { name: '계정 설정' }).click()
  const dialog = page.getByRole('dialog', { name: '계정 설정' })
  await expect(dialog.getByText('work-desktop (PowerShell)')).toBeVisible()
  await dialog.getByRole('button', { name: '연결 끊기' }).click()
  await expect(dialog.getByText('연결된 기기가 없습니다.')).toBeVisible()
  expect(calls).toEqual(['disconnect d1'])
})
