import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import { NOW, mockApi } from './fixtures/routes'

/**
 * 시각 회귀 (docs/ui-overhaul.md §8). 네 폭 × 두 테마 × 다섯 화면.
 *
 * 흔들리는 것은 고정한다: 시각(상대 시간·카운트다운)은 [NOW] 로, 애니메이션과 커서는 끈다. Monaco 는 가린다 —
 * 편집기 내부의 그리기는 우리 것이 아니고, 줄 높이 반 픽셀이 매번 다르다. 웹 글꼴이 다 온 뒤에 찍는다.
 */
const WIDTHS = [
  { name: '1440', width: 1440, height: 900 },
  { name: '1024', width: 1024, height: 768 },
  { name: '768', width: 768, height: 1024 },
  { name: '360', width: 360, height: 760 },
]

// `ready` 는 그 폭에서도 보이는 글자다 — 768 의 풀이 화면은 문제 창이 접힌 채 시작한다 (§8.1)
const SCREENS: { name: string; path: string; ready: string }[] = [
  { name: 'design', path: '/design', ready: '디자인 시스템' },
  { name: 'problems', path: '/problems', ready: '두 수의 합' },
  { name: 'solve', path: '/problems/two-sum/solve', ready: '두 수의 합' },
  { name: 'replay', path: '/submissions/s1/replay?step=2', ready: '단계 2' },
  { name: 'training', path: '/training', ready: '오늘 집중할 것' },
]

async function settle(page: Page, path: string, ready: string) {
  await expect(page.getByText(ready, { exact: false }).first()).toBeVisible({ timeout: 20_000 })
  // 카탈로그는 불러오는 중 상태(Skeleton)를 견본으로 늘 보여 준다 — 그 화면만 이 조건을 뺀다
  if (path !== '/design') await expect(page.locator('[aria-busy="true"]')).toHaveCount(0)
  await page.evaluate(() => document.fonts.ready)
}

for (const theme of ['light', 'dark'] as const) {
  for (const size of WIDTHS) {
    test.describe(`${theme} ${size.name}`, () => {
      test.use({ viewport: { width: size.width, height: size.height } })

      for (const screen of SCREENS) {
        test(screen.name, async ({ page }) => {
          await page.clock.setFixedTime(new Date(NOW))
          await mockApi(page, theme)
          await page.goto(screen.path)
          await settle(page, screen.path, screen.ready)
          await expect(page).toHaveScreenshot(`${screen.name}-${theme}-${size.name}.png`, {
            mask: [page.locator('.monaco-editor')],
          })
        })
      }
    })
  }
}
