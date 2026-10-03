/**
 * 번들 예산 (기술 설계서 §16.1, docs/production-readiness.md B5).
 *
 * 한때 배포물이 14MB 였다. 그중 9MB 는 **한 번도 로드되지 않는 워커**였다 —
 * `monaco-editor` 진입점이 TypeScript·CSS·HTML·JSON 언어 서비스를 함께 끌어왔고,
 * 그중 TypeScript 워커 하나가 6.9MB 였다. Kotlin·Java·Python 을 채점하는 제품에서다.
 *
 * 고치는 것보다 어려운 것은 고쳐 둔 채로 두는 것이다. 그 9MB 는 import 한 줄로
 * 돌아오고, 돌아와도 빌드는 성공한다. 그래서 크기를 시험한다.
 *
 * 한도는 "지금 값 + 여유"다. 넉넉하지만 사고를 잡을 만큼은 좁다.
 */
import { gzipSync } from 'node:zlib'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

const DIST = new URL('../dist/', import.meta.url).pathname

/**
 * 첫 화면이 내려받는 것. 에디터는 여기 들어 있지 않다 — Workspace 가 lazy 로 갈라
 * 두어, 문제 목록과 지문은 에디터를 기다리지 않고 그려진다.
 *
 * UI 개편 U0 에서 94KB → 117KB 가 됐다. 서버 상태 캐시(react-query) 10KB, 디자인 시스템과
 * 아이콘 7KB, 라우터(wouter) 3KB. 라우터는 react-router 를 먼저 들였다가 그것 하나가 32KB 라
 * 바꿨다 (docs/ui-overhaul.md §3). 웹 글꼴 CSS 는 진입에 넣지 않고 늦게 불러온다.
 * U1 에서 풀이 화면이 따로 청크로 나가며 102KB 로 줄었다. 한도는 그대로 둔다.
 */
const ENTRY_GZIP_KB = 140

/** 배포물 전체 (글꼴 제외). 언어 정의나 워커가 다시 쏟아지면 여기서 걸린다. */
const TOTAL_MB = 6

/**
 * 파일 개수.
 *
 * 크기만 재면 잡히지 않는 사고가 있다. 언어 정의 80여 개는 각각 3KB 라 합쳐도
 * 400KB 밖에 안 되지만, 그것이 들어 있다는 것은 진입점을 통째로 가져왔다는 뜻이고
 * 그러면 워커도 함께 와 있다.
 *
 * UI 개편에서 화면마다 따로 청크가 생기며(화면 하나에 JS·CSS 둘) 20 → 40 으로 올렸다.
 * 언어 정의가 쏟아지면 80개가 한꺼번에 늘므로 여전히 여기서 걸린다.
 */
const FILES = 40

/**
 * 웹 글꼴은 따로 센다. unicode-range 로 잘린 조각이라 파일은 100개 가깝지만 브라우저는
 * 페이지에 나온 글자가 든 조각만 받는다 — 위의 "전체"·"개수"와 같은 잣대로 재면 사고가
 * 아닌 것이 사고로 보이고, 섞어 두면 진짜 사고(워커가 돌아옴)를 가린다.
 */
const FONT_MB = 3.5

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    return entry.isDirectory() ? walk(path) : [path]
  })
}

const all = walk(DIST)
const isFont = (f) => f.endsWith('.woff2') || f.endsWith('.woff')
const files = all.filter((f) => !isFont(f))
const total = files.reduce((sum, f) => sum + statSync(f).size, 0)
const fonts = all.filter(isFont).reduce((sum, f) => sum + statSync(f).size, 0)

// 진입점은 index.html 이 직접 참조하는 것들이다. 이름으로 찾지 않는다 — 해시가 붙고,
// 청크 이름은 번들러가 정한다.
const html = readFileSync(join(DIST, 'index.html'), 'utf8')
const entry = [...html.matchAll(/(?:src|href)="\/assets\/([^"]+)"/g)].map((m) => m[1])
const entryGzip = entry.reduce(
  (sum, name) => sum + gzipSync(readFileSync(join(DIST, 'assets', name))).length,
  0,
)

const checks = [
  ['첫 화면 (gzip)', entryGzip / 1024, ENTRY_GZIP_KB, 'KB'],
  ['배포물 전체 (글꼴 제외)', total / 1024 / 1024, TOTAL_MB, 'MB'],
  ['파일 개수 (글꼴 제외)', files.length, FILES, '개'],
  ['웹 글꼴 전체', fonts / 1024 / 1024, FONT_MB, 'MB'],
]

let failed = false
for (const [label, actual, limit, unit] of checks) {
  const over = actual > limit
  failed ||= over
  const shown = unit === '개' ? actual : actual.toFixed(1)
  console.log(`${over ? '초과' : '통과'}  ${label}: ${shown}${unit} (한도 ${limit}${unit})`)
}

if (failed) {
  console.error('\n번들 예산을 넘었다. web/scripts/bundle-budget.mjs 가 한도와 그 이유를 적어 두었다.')
  console.error('의도한 증가라면 한도를 올리되, 무엇이 늘었는지 커밋 메시지에 남긴다.')
  process.exit(1)
}
