import { loader } from '@monaco-editor/react'
import * as monaco from 'monaco-editor/editor.js'
import editorWorker from 'monaco-editor/editor/editor.worker.js?worker'

// 에디터 기능(접기, 괄호 짝 맞추기, 여러 커서 …). 언어는 하나도 딸려 오지 않는다.
import 'monaco-editor/features/register.all.js'

// **`register.all.js` 가 진입점의 전부는 아니다.** 진입점이 따로 부르는 열 줄이 빠져
// 있고, 그 안에 찾기·자동완성·복사·붙여넣기·커서 명령이 들어 있다. 이름만 보고 전부인
// 줄 알았다가, 진입점의 import 목록과 한 줄씩 맞춰 보고 알았다. 빌드는 성공하고 화면도
// 멀쩡하니, 대조하지 않으면 눌러 보기 전에는 드러나지 않는다.
// monacoSetup.test.ts 가 그 대조를 대신한다 — 다음 monaco 판에서 또 갈라지면 실패한다.
//
// 진입점이 함께 부르는 codicon-modifiers.css 는 여기 없다. 패키지의 exports 가 .css 를
// 내보내지 않아 이름으로는 가져올 수 없고, suggest·codeAction 위젯이 이미 끌어온다.
import 'monaco-editor/editor/browser/coreCommands.js'
import 'monaco-editor/editor/common/standaloneStrings.js'
import 'monaco-editor/editor/contrib/caretOperations/browser/caretOperations.js'
import 'monaco-editor/editor/contrib/dropOrPasteInto/browser/copyPasteContribution.js'
import 'monaco-editor/editor/contrib/find/browser/findController.js'
import 'monaco-editor/editor/contrib/gotoError/browser/markerSelectionStatus.js'
import 'monaco-editor/editor/contrib/gotoSymbol/browser/goToCommands.js'
import 'monaco-editor/editor/contrib/semanticTokens/browser/documentSemanticTokens.js'
import 'monaco-editor/editor/contrib/suggest/browser/suggestController.js'

// 우리가 채점하는 언어만 (§16.1). 이 목록은 EDITOR_LANGUAGE 와 같아야 하며,
// monacoSetup.test.ts 가 둘이 어긋나면 실패한다.
import 'monaco-editor/languages/definitions/kotlin/register.js'
import 'monaco-editor/languages/definitions/java/register.js'
import 'monaco-editor/languages/definitions/python/register.js'

/**
 * Monaco 를 번들에서 쓴다.
 *
 * `@monaco-editor/react` 는 기본적으로 CDN 에서 에디터를 내려받는다. 외부 CDN 에 의존하면
 * 제한된 네트워크에서 에디터가 아예 뜨지 않고, 서드파티 스크립트가 사용자 코드가 있는
 * 페이지에서 실행된다 (§11.1 공급망). 번들에 넣어 두 문제를 함께 없앤다.
 *
 * **`monaco-editor` 를 통째로 들이지 않는다.** 그 진입점은 80여 개 언어 정의와 네 개의
 * 언어 서비스(TypeScript·CSS·HTML·JSON)를 함께 끌어온다. 언어 서비스는 각자 워커를
 * 하나씩 달고 오는데, TypeScript 워커 하나가 6.9MB 다 — Kotlin·Java·Python 을 채점하는
 * 제품이 배포물의 절반을 쓰지도 않을 TypeScript 컴파일러로 채우고 있었다.
 *
 * 그래서 세 조각으로 나눠 가져온다: 에디터 API(`editor.js`), 에디터 기능
 * (`features/register.all.js`), 그리고 **우리가 채점하는 언어의 문법 정의만**.
 */
export function setupMonaco() {
  self.MonacoEnvironment = {
    // 언어 서비스를 하나도 들이지 않으므로 워커는 기본 워커 하나뿐이다. 이 워커는
    // 문법 강조가 아니라 편집 연산(diff, 링크 감지 등)을 맡는다.
    getWorker: () => new editorWorker(),
  }
  loader.config({ monaco })
}
