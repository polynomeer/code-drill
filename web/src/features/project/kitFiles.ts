/**
 * 로컬 키트(ProjectKit)의 파일과 빌드 산출물 — 받은 폴더를 다시 올릴 때 빼고 읽는다.
 *
 * 키트 파일은 제출할 것이 아니다. 특히 `.codedrill/harness/` 를 올리면 Java·Kotlin 은 채점기의 하네스와 같은
 * 클래스가 두 번 생겨 컴파일이 깨진다. 목록은 서버의 `ProjectKit.KIT_PATHS` 와 같아야 한다.
 */
const KIT_FILES = new Set(['CODEDRILL.md', 'build.gradle.kts', 'settings.gradle.kts'])
const BUILD_DIRS = new Set(['build', 'out', '__pycache__', 'node_modules'])

/** `.` 으로 시작하는 경로(`.codedrill`·`.gitignore`·`.idea`·`.gradle`)도 뺀다 — 숨은 파일은 원래 가져오지 않는다 */
export function isKitOrBuildFile(path: string): boolean {
  const parts = path.split('/')
  if (parts.some((part) => part.startsWith('.'))) return true
  if (parts.length === 1 && KIT_FILES.has(path)) return true
  if (parts.slice(0, -1).some((dir) => BUILD_DIRS.has(dir))) return true
  return path.endsWith('.class')
}
