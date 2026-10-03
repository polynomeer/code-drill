import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

/**
 * 로그인 전 화면. 백엔드 없이 그려지는 유일한 제품 화면이다.
 *
 * 세션이 없으면 SessionGate 가 로그인 화면을 그린다 (app/router.tsx). 가입 전환과 접근성만
 * 본다 — 실제 로그인은 scripts/smoke.py 가 API 로 확인한다.
 */
test('로그인 화면에 접근성 위반이 없다', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})

test('가입으로 바꾸면 표시 이름 칸이 생긴다', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: /가입하기/ }).click()
  await expect(page.getByRole('button', { name: '가입하고 시작' })).toBeVisible()
  await expect(page.getByPlaceholder('비워 두면 이메일 앞부분을 쓴다')).toBeVisible()
})
