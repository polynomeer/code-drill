import { defineConfig, devices } from '@playwright/test'

declare const process: { env: Record<string, string | undefined> }

/**
 * 성능 게이트 (docs/ui-overhaul.md §8 — LCP < 2.5s, INP < 200ms, Workspace 조작 100ms 이내, UI §11.2).
 *
 * **배포물(`vite build`)을 잰다.** 개발 서버는 모듈을 하나씩 변환해 보내 첫 그림이 몇 배 느리고, 그 숫자는
 * 사용자가 겪는 것이 아니다. 그래서 `pnpm build` 뒤에 `vite preview` 로 띄운다. API 는 흉내 낸다 — 서버가
 * 아니라 화면의 속도를 잰다 (서버 지연은 §12.1 SLO 와 scripts/loadtest.py 의 몫).
 */
const PORT = 4182

export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.perf.ts',
  // 숫자를 재는 시험은 서로 CPU 를 다투면 안 된다 — 하나씩
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: `http://localhost:${PORT}`,
  },
  webServer: {
    command: `pnpm exec vite preview --port ${PORT} --strictPort`,
    port: PORT,
    reuseExistingServer: false,
  },
})
