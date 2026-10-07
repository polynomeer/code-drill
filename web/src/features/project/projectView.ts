import type { ProjectSubmission } from '../../shared/types'

/**
 * 프로젝트형 작업 공간의 표현 규칙 (docs/ui-overhaul.md §6.11). 화면 없이 시험한다.
 */

/** 파일 트리의 한 줄 — 폴더는 그 아래 파일보다 먼저, 깊이만큼 들여 쓴다 */
export interface TreeRow {
  path: string
  name: string
  depth: number
  folder: boolean
}

/** 경로 목록을 폴더·파일 줄로. 폴더는 따로 저장하지 않으므로(빈 폴더는 없다) 파일 경로에서 만든다 */
export function fileTree(paths: string[]): TreeRow[] {
  const rows: TreeRow[] = []
  const seen = new Set<string>()
  for (const path of [...paths].sort(compareTreeOrder)) {
    const parts = path.split('/')
    for (let depth = 0; depth < parts.length - 1; depth++) {
      const folder = parts.slice(0, depth + 1).join('/')
      if (seen.has(folder)) continue
      seen.add(folder)
      rows.push({ path: folder, name: parts[depth]!, depth, folder: true })
    }
    rows.push({ path, name: parts[parts.length - 1]!, depth: parts.length - 1, folder: false })
  }
  return rows
}

/** 같은 폴더 안에서는 하위 폴더가 파일보다 먼저 — 편집기의 탐색기와 같은 순서 */
function compareTreeOrder(a: string, b: string): number {
  const pa = a.split('/')
  const pb = b.split('/')
  for (let i = 0; i < Math.min(pa.length, pb.length); i++) {
    if (pa[i] === pb[i]) continue
    const aFolder = i < pa.length - 1
    const bFolder = i < pb.length - 1
    if (aFolder !== bFolder) return aFolder ? -1 : 1
    return pa[i]! < pb[i]! ? -1 : 1
  }
  return pa.length - pb.length
}

/** 요구사항 본문. 첫 줄의 `# 제목` 은 툴바가 이미 말한다 — 두 번 보이면 제목이 둘이다 */
export function statementBody(markdown: string, title: string): string {
  const [first, ...rest] = markdown.split('\n')
  return first?.trim() === `# ${title}` ? rest.join('\n').replace(/^\s+/, '') : markdown
}

/**
 * 처음 열 파일. 시작 저장소는 고칠 자리를 TODO 로 비워 두니 TODO 가 있는 소스가 그것이고, 없으면(초안에서
 * 이미 지웠으면) 테스트와 패키지 선언이 아닌 첫 파일이다.
 */
export function firstFile(files: Record<string, string>): string | null {
  const paths = Object.keys(files).sort()
  const sources = paths.filter((path) => !path.startsWith('tests/') && !path.endsWith('__init__.py'))
  return sources.find((path) => (files[path] ?? '').includes('TODO')) ?? sources[0] ?? paths[0] ?? null
}

/** 채점 단계. 서버가 아는 상태는 셋뿐이라 단계도 셋이다 — 빌드와 테스트를 지어내서 가르지 않는다 */
export const JUDGING_STEPS = ['대기', '빌드와 테스트', '결과'] as const

export function judgingStep(status: ProjectSubmission['status']): number {
  return status === 'QUEUED' ? 0 : status === 'LEASED' ? 1 : 2
}

/**
 * 숨은 테스트의 칸. 서버는 통과 수만 보낸다 — 어느 테스트인지는 모르므로 앞에서부터 칠한다. 칸이 말하는 것은
 * 수뿐이고, 글로도 "몇 개 중 몇 개"라고 같이 적는다 (CaseGrid 의 숨은 그룹과 같은 규칙).
 */
export function hiddenCells(passed: number | null, total: number | null): boolean[] {
  if (total === null || total <= 0) return []
  const ok = Math.max(0, Math.min(passed ?? 0, total))
  return Array.from({ length: total }, (_, index) => index < ok)
}
