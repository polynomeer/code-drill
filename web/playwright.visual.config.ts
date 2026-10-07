import { defineConfig, devices } from '@playwright/test'

declare const process: { env: Record<string, string | undefined> }

/**
 * 시각 회귀 (docs/ui-overhaul.md §8 — `/design` 과 주요 라우트, 1440·1024·768·360 네 폭 × 두 테마).
 *
 * **기준 이미지는 리눅스 컨테이너에서만 만든다.** 글자 그리기는 운영체제마다 조금씩 달라 macOS 에서 찍은 기준은
 * CI 에서 맞지 않는다. 그래서 이 설정은 공식 Playwright 이미지 안에서만 돌린다 — 로컬은 `pnpm e2e:visual`
 * (scripts/visual.sh 가 같은 이미지를 띄운다), CI 는 같은 이미지를 컨테이너로 쓴다. 기본 `pnpm e2e` 에는 들지
 * 않는다 (파일 이름이 `*.visual.ts`).
 *
 * 화면이 일부러 바뀌었으면 `pnpm e2e:visual --update-snapshots` 로 기준을 다시 찍고, 무엇이 바뀌었는지 커밋에 적는다.
 */
const PORT = 4181

export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.visual.ts',
  // 기준 이미지에 운영체제 이름을 붙이지 않는다 — 언제나 같은 컨테이너에서 찍는다
  snapshotPathTemplate: '{testDir}/__screenshots__/{testFileName}/{arg}{ext}',
  fullyParallel: true,
  // Monaco 를 띄우는 화면이 여럿이다 — 컨테이너에서 한꺼번에 돌리면 첫 그림이 시간 안에 오지 않는다
  workers: 2,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? 'github' : 'list',
  expect: {
    // 찍기 전에 웹 글꼴을 다시 기다린다. 퀘스트 테마의 제목 글꼴까지 더해지자 두 워커에서 5초를 넘긴 적이 있다
    timeout: 15_000,
    toHaveScreenshot: {
      animations: 'disabled',
      caret: 'hide',
      // 안티에일리어싱 몇 점까지는 같은 화면이다 — 한 칸 어긋난 테두리는 이보다 훨씬 크다
      maxDiffPixelRatio: 0.002,
    },
  },
  use: {
    ...devices['Desktop Chrome'],
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
  },
  webServer: {
    command: `pnpm exec vite --port ${PORT} --strictPort --host 0.0.0.0`,
    port: PORT,
    reuseExistingServer: false,
  },
})
