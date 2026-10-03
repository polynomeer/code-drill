/**
 * 토큰 대비 검사 (디자인 설계서 §15.2, docs/ui-overhaul.md §5.1·§8).
 *
 * 색을 바꾸는 사람은 대개 한 테마만 보고 바꾼다. 다크에서 고른 값이 라이트에서 4.5:1 을
 * 깨도 빌드는 성공하고 화면도 "보이기는" 한다. 그래서 토큰 쌍을 두 테마 모두에서 잰다.
 *
 * tokens.css 를 직접 읽는다. 값을 여기 옮겨 적으면 둘이 어긋난다.
 */
import { readFileSync } from 'node:fs'

const css = readFileSync(new URL('../src/design/tokens.css', import.meta.url), 'utf8')

/** `[data-theme='dark'] { ... }` 같은 블록에서 `--color-*: #hex` 를 모은다. */
function block(selector) {
  const start = css.indexOf(selector)
  if (start < 0) throw new Error(`tokens.css 에 ${selector} 블록이 없다`)
  const body = css.slice(css.indexOf('{', start) + 1, css.indexOf('}', start))
  return Object.fromEntries([...body.matchAll(/--(color-[\w-]+):\s*(#[0-9a-f]{6})\b/gi)].map((m) => [m[1], m[2]]))
}

const themes = {
  light: block("[data-theme='light']"),
  dark: block("[data-theme='dark']"),
}

function luminance(hex) {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function ratio(a, b) {
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p)
  return (x + 0.05) / (y + 0.05)
}

const TEXT = 4.5 // 일반 글자
const UI = 3 // 컨트롤 경계, 의미 있는 그래픽

const surfaces = ['color-bg-app', 'color-bg-surface', 'color-bg-elevated']
const pairs = [
  // 글자로 쓰이는 색은 모든 표면 위에서 4.5:1
  ...[
    'color-text-primary',
    'color-text-muted',
    'color-brand',
    'color-success',
    'color-warning',
    'color-danger',
    'color-system',
    'color-difficulty-1',
    'color-difficulty-2',
    'color-difficulty-3',
    'color-difficulty-4',
    'color-difficulty-5',
  ].flatMap((fg) => surfaces.map((bg) => [fg, bg, TEXT])),
  // 옅은 배경 위 같은 계열 글자 (배지·알림)
  ['color-brand', 'color-brand-subtle', TEXT],
  ['color-success', 'color-success-subtle', TEXT],
  ['color-warning', 'color-warning-subtle', TEXT],
  ['color-danger', 'color-danger-subtle', TEXT],
  ['color-system', 'color-system-subtle', TEXT],
  ['color-text-primary', 'color-trace-subtle', TEXT],
  ['color-text-primary', 'color-bg-subtle', TEXT],
  ['color-text-muted', 'color-bg-subtle', TEXT],
  ['color-text-primary', 'color-bg-inset', TEXT],
  ['color-text-on-brand', 'color-brand', TEXT],
  ['color-text-on-brand', 'color-brand-hover', TEXT],
  // 경계와 그래픽 — 3:1
  ...surfaces.map((bg) => ['color-border-strong', bg, UI]),
  ...surfaces.map((bg) => ['color-trace', bg, UI]),
]

let failed = 0
for (const [theme, tokens] of Object.entries(themes)) {
  for (const [fg, bg, min] of pairs) {
    if (!tokens[fg] || !tokens[bg]) {
      console.error(`없음  ${theme}: --${fg} 또는 --${bg}`)
      failed++
      continue
    }
    const value = ratio(tokens[fg], tokens[bg])
    if (value < min) {
      console.error(`미달  ${theme}: --${fg} on --${bg} = ${value.toFixed(2)}:1 (기준 ${min}:1)`)
      failed++
    }
  }
}

if (failed) {
  console.error(`\n대비 검사 ${failed}건 실패. web/src/design/tokens.css 의 값을 고친다.`)
  process.exit(1)
}
console.log(`통과  토큰 대비: ${pairs.length}쌍 × ${Object.keys(themes).length}테마`)
