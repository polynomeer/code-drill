import { defineConfig, devices } from '@playwright/test'

// vite.config.ts 와 같은 이유로 @types/node 대신 쓰는 것만 선언한다.
declare const process: { env: Record<string, string | undefined> }

/**
 * 브라우저 검사 (docs/ui-overhaul.md §8 — E2E·접근성·시각 회귀의 자리).
 *
 * 백엔드 없이 돈다 — 컴포넌트 카탈로그(/design), 로그인, 그리고 API 를 흉내 낸 풀이 화면.
 * 채점까지 끝까지 도는 E2E 는 scripts/smoke.py 처럼 앱 셋이 떠 있어야 해서 아직 없다.
 *
 * 파일 이름이 `*.e2e.ts` 인 이유: vitest 가 `*.spec.ts` 를 자기 것으로 집어 간다.
 */
const PORT = 4179

export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    command: `pnpm exec vite --port ${PORT} --strictPort`,
    port: PORT,
    reuseExistingServer: !process.env.CI,
  },
})
