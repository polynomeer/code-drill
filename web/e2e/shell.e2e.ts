import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

/**
 * 로그인 화면 `/login`. 가입 전환과 접근성만 본다 — 실제 로그인은 scripts/smoke.py 가 API 로 확인한다.
 * 로그인이 필요한 화면에 둘러보던 사람이 오면 여기로 보낸다 (app/router.tsx).
 */
test('로그인 화면에 접근성 위반이 없다', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()
  expect(result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
})

test('가입으로 바꾸면 표시 이름 칸이 생긴다', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: /가입하기/ }).click()
  await expect(page.getByRole('button', { name: '가입하고 시작' })).toBeVisible()
  await expect(page.getByPlaceholder('비워 두면 이메일 앞부분을 쓴다')).toBeVisible()
})

test('로그인이 필요한 화면은 돌아올 주소를 들고 로그인으로 간다', async ({ page }) => {
  await page.goto('/problems/two-sum/solve?lang=JAVA')
  await expect(page).toHaveURL(/\/login\?next=%2Fproblems%2Ftwo-sum%2Fsolve%3Flang%3DJAVA$/)
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
})
