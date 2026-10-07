import type { GroupInfo, GroupResult, Verdict } from '../../shared/types'

/**
 * 케이스 격자 — 테스트 케이스를 칸으로 늘어놓은 판정 그림 (docs/ui-overhaul.md §6.3).
 *
 * 칸 하나가 케이스 하나다. **공개 그룹만 케이스별로 안다.** 숨은 그룹은 서버가 케이스를 비워 보내므로
 * (§8.3) 그룹 판정과 점수만으로 칠한다 — 몇 번째 케이스에서 떨어졌는지는 그리지 않는다. 그리면
 * 숨은 입력의 순서를 짐작하게 하고, 무엇보다 모르는 것을 아는 것처럼 보인다 (No false precision).
 */
export type CellState = 'passed' | 'failed' | 'limit' | 'system' | 'pending'

export interface CaseCell {
  state: CellState
  /** 공개 케이스일 때만 */
  caseId?: string
  /** 아래 입력·기댓값·실행값이 보여 주는 그 케이스 */
  first?: boolean
}

export interface CaseRow {
  groupId: string
  hidden: boolean
  cells: CaseCell[]
  /** 칸이 너무 많아 접은 수 */
  folded: number
  verdict: Verdict | null
  score: number
  maxScore: number
  /** 스크린리더와 칸 옆 글 — 색을 못 봐도 같은 것을 읽는다 */
  label: string
}

/** 한 줄에 그리는 칸의 상한. 케이스가 수백 개인 그룹이 화면을 덮지 않게 한다 */
export const MAX_CELLS = 48

export function cellState(verdict: Verdict): CellState {
  switch (verdict) {
    case 'ACCEPTED':
      return 'passed'
    case 'WRONG_ANSWER':
    case 'RUNTIME_ERROR':
      return 'failed'
    case 'TIME_LIMIT':
    case 'MEMORY_LIMIT':
    case 'OUTPUT_LIMIT':
      return 'limit'
    default:
      return 'system'
  }
}

/**
 * 판정이 나온 제출의 줄들. `info` 는 문제의 그룹 정의 — 숨은 그룹의 케이스 수는 여기서만 안다.
 * 없으면 숨은 그룹은 칸 하나로 그린다.
 */
export function caseRows(groups: GroupResult[], info: GroupInfo[] | undefined): CaseRow[] {
  let firstMarked = false
  return groups.map((group) => {
    const count = info?.find((item) => item.id === group.groupId)?.caseCount ?? 0
    const hidden = group.cases.length === 0
    let cells: CaseCell[]
    let label: string

    if (!hidden) {
      cells = group.cases.map((item) => {
        const state = cellState(item.verdict)
        const first = !firstMarked && state !== 'passed'
        if (first) firstMarked = true
        return { state, caseId: item.caseId, first }
      })
      const passed = cells.filter((cell) => cell.state === 'passed').length
      label = `공개 케이스 ${cells.length}개 중 ${passed}개 통과`
    } else {
      const size = Math.max(count, 1)
      const state = cellState(group.verdict)
      if (state === 'passed') {
        cells = Array.from({ length: size }, () => ({ state }))
        label = count > 0 ? `숨은 케이스 ${count}개 모두 통과` : '숨은 케이스 모두 통과'
      } else if (group.maxScore > 0 && group.score > 0) {
        // 부분 점수(SUM) — 점수가 이미 말한 만큼만 칠한다. 어느 칸인지는 모르므로 앞에서부터 채운다
        const cleared = Math.floor((size * group.score) / group.maxScore)
        cells = Array.from({ length: size }, (_, index) => ({ state: index < cleared ? 'passed' : state }))
        label = `숨은 케이스${count > 0 ? ` ${count}개` : ''} — 점수 ${group.score} / ${group.maxScore}`
      } else {
        cells = Array.from({ length: size }, () => ({ state }))
        label = `숨은 케이스${count > 0 ? ` ${count}개` : ''} — 통과하지 못함`
      }
    }

    const folded = Math.max(0, cells.length - MAX_CELLS)
    if (folded > 0) {
      // 접어도 첫 실패 칸은 남긴다 — 아래 diff 가 그 케이스를 말한다
      const firstIndex = cells.findIndex((cell) => cell.first)
      const kept = cells.slice(0, MAX_CELLS)
      if (firstIndex >= MAX_CELLS) kept[MAX_CELLS - 1] = cells[firstIndex]!
      cells = kept
    }
    return { groupId: group.groupId, hidden, cells, folded, verdict: group.verdict, score: group.score, maxScore: group.maxScore, label }
  })
}

/** 채점 중 — 문제의 그룹 정의로 아직 치지 않은 칸만 늘어놓는다 */
export function pendingRows(info: GroupInfo[]): CaseRow[] {
  return info.map((group) => {
    const size = Math.max(group.caseCount, 1)
    return {
      groupId: group.id,
      hidden: false,
      cells: Array.from({ length: Math.min(size, MAX_CELLS) }, () => ({ state: 'pending' as const })),
      folded: Math.max(0, size - MAX_CELLS),
      verdict: null,
      score: 0,
      maxScore: group.weight,
      label: `케이스 ${group.caseCount}개 채점 중`,
    }
  })
}
