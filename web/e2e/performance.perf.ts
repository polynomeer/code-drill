import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import { mockApi } from './fixtures/routes'

/**
 * 성능 게이트 (docs/ui-overhaul.md §8, UI 디자인 문서 §11.2 "일반 조작 100ms 이내").
 *
 * - LCP: CPU 를 4배 느리게(Lighthouse 모바일과 같은 값) 하고 문제 목록·풀이 화면의 가장 큰 그림이 2.5초 안에.
 * - INP: 실제 조작(정렬 바꾸기, 탭 바꾸기)에서 Event Timing 이 잰 가장 긴 상호작용이 200ms 안에.
 * - Workspace: 결과 창 접기(⌘J)·문제 탭 바꾸기가 100ms 안에 (CPU 그대로).
 *
 * Chromium 의 성능 API 를 쓴다 — 다른 브라우저는 Event Timing 이 없거나 다르다.
 */
async function throttle(page: Page, rate: number) {
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Emulation.setCPUThrottlingRate', { rate })
}

/** 페이지가 시작된 뒤의 LCP(ms). 버퍼에 쌓인 것까지 본다. */
async function lcp(page: Page): Promise<number> {
  return page.evaluate(
    () =>
      new Promise<number>((resolve) => {
        let last = 0
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) last = entry.startTime
        }).observe({ type: 'largest-contentful-paint', buffered: true })
        setTimeout(() => resolve(last), 1000)
      }),
  )
}

/** 지금부터 쌓이는 상호작용(Event Timing)의 가장 긴 길이를 모은다. */
async function watchInteractions(page: Page) {
  await page.evaluate(() => {
    const w = window as unknown as { __longest: number }
    w.__longest = 0
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries() as (PerformanceEventTiming & { interactionId?: number })[]) {
        if (entry.interactionId) w.__longest = Math.max(w.__longest, entry.duration)
      }
    }).observe({ type: 'event', buffered: false, durationThreshold: 16 } as PerformanceObserverInit)
  })
  return async () => {
    // Event Timing 은 다음 그림이 끝난 뒤에 온다 — 한 박자 기다린다
    await page.waitForTimeout(300)
    return page.evaluate(() => (window as unknown as { __longest: number }).__longest)
  }
}

test('문제 목록 — LCP 2.5초, 정렬 바꾸기 INP 200ms', async ({ page }) => {
  await mockApi(page, 'light')
  await throttle(page, 4)
  await page.goto('/problems')
  await expect(page.getByRole('link', { name: '두 수의 합' }).first()).toBeVisible()
  const largest = await lcp(page)
  console.log(`LCP /problems (CPU×4): ${Math.round(largest)}ms`)
  expect(largest).toBeLessThan(2500)

  const longest = await watchInteractions(page)
  await page.getByRole('button', { name: /제목/ }).first().click()
  await page.getByRole('button', { name: /난이도/ }).first().click()
  const inp = await longest()
  console.log(`INP /problems 정렬 (CPU×4): ${Math.round(inp)}ms`)
  expect(inp).toBeLessThan(200)
})

test('풀이 화면 — LCP 2.5초, 결과 창 접기·탭 바꾸기 100ms', async ({ page }) => {
  await mockApi(page, 'light')
  await throttle(page, 4)
  await page.goto('/problems/two-sum/solve')
  await expect(page.getByText('정수 배열에서', { exact: false })).toBeVisible()
  const largest = await lcp(page)
  console.log(`LCP solve (CPU×4): ${Math.round(largest)}ms`)
  expect(largest).toBeLessThan(2500)

  // Workspace 조작은 기기 그대로 잰다 (UI §11.2 "일반 조작 100ms 이내")
  await throttle(page, 1)
  await expect(page.locator('.monaco-editor')).toBeVisible({ timeout: 15_000 })
  const longest = await watchInteractions(page)
  await page.getByRole('tab', { name: '해설' }).click()
  await page.getByRole('tab', { name: '문제', exact: true }).click()
  await page.locator('body').press('ControlOrMeta+j')
  await page.locator('body').press('ControlOrMeta+j')
  const inp = await longest()
  console.log(`Workspace 조작: ${Math.round(inp)}ms`)
  expect(inp).toBeLessThan(100)
})
