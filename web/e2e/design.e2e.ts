import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 디자인 시스템 카탈로그 (/design).
 *
 * 컴포넌트의 모든 상태가 한 화면에 있으므로, 여기서 접근성 위반이 0 이면 컴포넌트 자체는
 * 깨끗하다. 화면에서 위반이 나오면 컴포넌트가 아니라 쓰는 쪽을 본다.
 */
async function axe(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)
}

for (const theme of ['light', 'dark'] as const) {
  test(`${theme} 테마에서 접근성 위반이 없다`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme })
    await page.goto('/design')
    await expect(page.getByRole('heading', { name: '디자인 시스템' })).toBeVisible()
    await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
    expect(await axe(page)).toEqual([])
  })
}

test('가로로 넘치지 않는다', async ({ page }) => {
  await page.goto('/design')
  // innerWidth 와 견주면 안 된다. 모바일 브라우저는 넘친 만큼 레이아웃 폭을 스스로 넓혀서
  // innerWidth 도 함께 커진다 — 둘을 빼면 언제나 0 이다. 기기 폭과 견준다.
  const width = page.viewportSize()?.width ?? 0
  const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth)
  expect(scrollWidth).toBeLessThanOrEqual(width)
})

test('탭은 화살표로 옮기고 비활성 탭을 건너뛴다', async ({ page }) => {
  await page.goto('/design')
  const tabs = page.getByRole('tablist', { name: '문제 pane' })
  await tabs.getByRole('tab', { name: '해설' }).click()
  await page.keyboard.press('ArrowRight')
  // '풀이'는 비활성이라 건너뛰고 '제출'로 간다.
  await expect(tabs.getByRole('tab', { name: '제출' })).toBeFocused()
  await expect(tabs.getByRole('tab', { name: '제출' })).toHaveAttribute('aria-selected', 'true')
  await page.keyboard.press('Home')
  await expect(tabs.getByRole('tab', { name: '문제' })).toHaveAttribute('aria-selected', 'true')
})

test('대화상자는 Esc 로 닫히고 연 버튼으로 포커스가 돌아온다', async ({ page }) => {
  await page.goto('/design')
  const opener = page.getByRole('button', { name: '대화상자 열기' })
  await opener.click()
  const dialog = page.getByRole('dialog', { name: '코드를 초기화할까요?' })
  await expect(dialog).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
  await expect(opener).toBeFocused()
})

test('테마 선택은 새로고침 뒤에도 남는다', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' })
  await page.goto('/design')
  await page.getByLabel('테마').selectOption('dark')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.reload()
  // 첫 페인트 전 인라인 스크립트가 적용한다 (index.html)
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})
