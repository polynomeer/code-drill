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
 */
const ENTRY_GZIP_KB = 120

/** 배포물 전체. 언어 정의나 워커가 다시 쏟아지면 여기서 걸린다. */
const TOTAL_MB = 6

/**
 * 파일 개수.
 *
 * 크기만 재면 잡히지 않는 사고가 있다. 언어 정의 80여 개는 각각 3KB 라 합쳐도
 * 400KB 밖에 안 되지만, 그것이 들어 있다는 것은 진입점을 통째로 가져왔다는 뜻이고
 * 그러면 워커도 함께 와 있다.
 */
const FILES = 20

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    return entry.isDirectory() ? walk(path) : [path]
  })
}

const files = walk(DIST)
const total = files.reduce((sum, f) => sum + statSync(f).size, 0)

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
  ['배포물 전체', total / 1024 / 1024, TOTAL_MB, 'MB'],
  ['파일 개수', files.length, FILES, '개'],
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
