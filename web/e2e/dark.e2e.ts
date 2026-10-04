import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { mockApi } from './fixtures/routes'

/**
 * 다크 테마 최종 검수 (docs/ui-overhaul.md §9 U9, UI 디자인 문서 §11.3 "다크 테마에서 status·trace 색상의 최종
 * 대비 검증"). 토큰 쌍은 `scripts/contrast.mjs` 가 재지만, 실제 화면에서 겹친 바탕(옅은 상태색 위의 보조 글자,
 * 막대 위의 배지)은 화면을 그려 봐야 안다. 주요 라우트를 다크로 그려 axe 로 잰다.
 */
const ROUTES: [string, string][] = [
  ['/problems', '두 수의 합'],
  ['/problems/two-sum', '정수 배열에서'],
  ['/training', '오늘 집중할 것'],
  ['/competencies?c=MODELING', '도움 받음'],
  ['/contests', '주간 대회'],
  ['/contests/c1', '순위표'],
  ['/submissions', '두 수의 합'],
  ['/submissions/s1', '예제 01 에서 틀렸습니다'],
  ['/submissions/s1/replay?step=2', '단계 2'],
  ['/u/ada', '최근 1년 활동'],
  ['/me', '처방 요약·프로젝트'],
  ['/admin/rejudges', '케이스 수정'],
  ['/welcome', '세 가지만 묻겠습니다'],
]

for (const [path, ready] of ROUTES) {
  test(`다크 — ${path} 에 대비·접근성 위반이 없다`, async ({ page }) => {
    await mockApi(page)
    await page.goto(path)
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(page.getByText(ready, { exact: false }).first()).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('[aria-busy="true"]')).toHaveCount(0)
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).exclude('.monaco-editor').analyze()
    expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
}
