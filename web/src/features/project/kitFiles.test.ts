import { describe, expect, it } from 'vitest'
import { isKitOrBuildFile } from './kitFiles'

describe('isKitOrBuildFile', () => {
  it('키트 파일과 빌드 산출물은 뺀다', () => {
    for (const path of [
      '.codedrill/harness/codedrill/CodedrillHarness.java',
      '.codedrill/project.json',
      '.gitignore',
      'CODEDRILL.md',
      'build.gradle.kts',
      'settings.gradle.kts',
      'build/classes/kotlin/main/queue/JobQueue.class',
      'cart/__pycache__/pricing.cpython-312.pyc',
      '.idea/workspace.xml',
      'src/queue/JobQueue.class',
    ]) {
      expect(isKitOrBuildFile(path), path).toBe(true)
    }
  })

  it('사용자의 소스와 테스트는 그대로 둔다 — 하위 폴더의 같은 이름도', () => {
    for (const path of ['src/queue/JobQueue.kt', 'tests/MyEdgeTest.kt', 'cart/pricing.py', 'docs/CODEDRILL.md', 'src/builder/Tool.java']) {
      expect(isKitOrBuildFile(path), path).toBe(false)
    }
  })
})
