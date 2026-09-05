import { loader } from '@monaco-editor/react'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/editor/editor.worker.js?worker'

/**
 * Monaco 를 번들에서 쓴다.
 *
 * `@monaco-editor/react` 는 기본적으로 CDN 에서 에디터를 내려받는다. 외부 CDN 에 의존하면
 * 제한된 네트워크에서 에디터가 아예 뜨지 않고, 서드파티 스크립트가 사용자 코드가 있는
 * 페이지에서 실행된다 (§11.1 공급망). 번들에 넣어 두 문제를 함께 없앤다.
 */
export function setupMonaco() {
  self.MonacoEnvironment = {
    // Kotlin 은 Monaco 에 언어 서비스가 없어 기본 워커 하나면 충분하다.
    getWorker: () => new editorWorker(),
  }
  loader.config({ monaco })
}
