import { describe, expect, it } from 'vitest'
// Vite 가 파일 내용을 문자열로 넘겨준다. node:fs 를 쓰면 @types/node 가 필요해지는데,
// 이 프로젝트는 한 줄 때문에 타입 패키지를 들이지 않는다 (vite.config.ts 와 같은 판단).
import source from './monacoSetup.ts?raw'
import entry from 'monaco-editor/index.js?raw'
import registerAll from 'monaco-editor/features/register.all.js?raw'
import { EDITOR_LANGUAGE } from '../../shared/types'

/**
 * 번들에 든 언어 = 우리가 채점하는 언어 (§16.1).
 *
 * `monaco-editor` 를 통째로 가져오면 80여 개 언어 정의와 네 개의 언어 서비스가 딸려
 * 오고, 그중 TypeScript 워커 하나가 6.9MB 다. 그래서 필요한 것만 골라 가져오는데,
 * **골라 가져오는 코드는 조용히 되돌아간다** — 자동완성 하나를 붙이려다 `monaco-editor`
 * 를 통째로 import 하면 아무 경고 없이 9MB 가 돌아온다.
 *
 * 그래서 소스 자체를 읽어 검사한다. 언어를 늘리면 EDITOR_LANGUAGE 와 import 가 함께
 * 움직여야 하고, 한쪽만 고치면 여기서 걸린다. 런타임을 띄워 확인하려면 DOM 이 필요한데,
 * 정작 지키려는 것은 "무엇을 import 했는가"라서 소스를 보는 편이 더 정확하다.
 */
describe('monacoSetup', () => {
  const imported = [...source.matchAll(/monaco-editor\/languages\/definitions\/([\w-]+)\/register/g)]
    .map((match) => match[1])

  it('채점 언어의 문법 정의를 전부 가져온다', () => {
    expect(imported.sort()).toEqual(Object.values(EDITOR_LANGUAGE).sort())
  })

  it('언어 서비스는 하나도 가져오지 않는다', () => {
    // TypeScript·CSS·HTML·JSON 서비스다. 각자 워커를 달고 오며, 넷을 합치면 9MB 다.
    // Kotlin·Java·Python 에는 어차피 붙지 않는다.
    expect(source).not.toMatch(/monaco-editor\/languages\/features/)
  })

  it('monaco-editor 진입점을 통째로 가져오지 않는다', () => {
    // `from 'monaco-editor'` 한 줄이 위의 둘을 모두 되돌린다.
    expect(source).not.toMatch(/from 'monaco-editor'/)
  })
})

/**
 * 언어만 뺀다. 그 밖의 것은 진입점과 같아야 한다.
 *
 * `features/register.all.js` 하나면 되는 줄 알았는데 아니었다. 진입점은 그것 말고도
 * 열 줄을 더 부르고, 그 안에 **찾기·자동완성·복사·붙여넣기·커서 명령**이 들어 있다.
 * 빠뜨려도 빌드는 성공하고, 화면도 멀쩡하고, 편집도 된다 — 없어진 것은 눌러 보기 전에는
 * 보이지 않는 기능이라, 진입점과 한 줄씩 맞춰 보기 전에는 드러나지 않았다.
 *
 * 그래서 사람이 세지 않는다. 진입점이 부르는 목록에서 언어를 뺀 것이 우리가 부르는
 * 목록과 같은지 대조한다. monaco 를 올릴 때 기능이 하나 늘면 여기서 걸린다.
 */
describe('monaco 기능 목록', () => {
  /** `esm/vs` 기준 상대 경로로 맞춘다. 세 곳이 각자 다른 기준으로 적혀 있다. */
  const specifiers = (from: string, base: string) =>
    [...from.matchAll(/^import '([^']+)'/gm)].flatMap(([, path]) => {
      if (path === undefined) return []
      if (path.startsWith('../')) return [path.slice(3)]
      if (path.startsWith('./')) return [base + path.slice(2)]
      return [path.replace(/^monaco-editor\//, '')]
    })

  // 언어 정의와 언어 서비스가 뺀 것이다. 워커를 달고 오는 쪽은 언어 서비스다.
  const isLanguage = (path: string) => path.startsWith('languages/')

  it('진입점의 기능을 하나도 빠뜨리지 않는다', () => {
    const wanted = specifiers(entry, '').filter((p) => !isLanguage(p) && !p.endsWith('.css'))
    const have = new Set([
      ...specifiers(registerAll, 'features/'),
      ...specifiers(source, ''),
    ])

    expect(wanted.filter((p) => !have.has(p))).toEqual([])
  })
})
