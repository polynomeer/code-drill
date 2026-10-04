import type { SubmissionLanguage } from '../../shared/types'

/**
 * 로그인 전 초안 — 이 기기에만 산다 (디자인 설계서 §11.1, docs/ui-overhaul.md §6.9).
 *
 * 둘러보던 사람이 풀이 화면에서 코드를 쓰기 시작하면 서버에 둘 곳이 없다. 여기 둔다. 로그인하면 풀이
 * 화면이 이것을 서버 초안과 견주어 — 서버 초안이 없으면 이어 쓰고, 다르면 어느 쪽으로 갈지 묻는다.
 *
 * 저장소를 못 쓰는 브라우저(사생활 보호 모드)에서는 조용히 아무것도 하지 않는다 — 그때는 탭을 닫으면
 * 사라진다는 것을 화면이 말한다.
 */
const PREFIX = 'codedrill.local-draft.'

function key(problemId: string, language: SubmissionLanguage): string {
  return `${PREFIX}${problemId}.${language}`
}

export function readLocalDraft(problemId: string, language: SubmissionLanguage): string | null {
  try {
    return localStorage.getItem(key(problemId, language))
  } catch {
    return null
  }
}

/** 적었는지. 못 적었으면 false — 화면이 "이 기기에 저장됨"이라고 말하면 안 된다. */
export function writeLocalDraft(problemId: string, language: SubmissionLanguage, code: string): boolean {
  try {
    localStorage.setItem(key(problemId, language), code)
    return true
  } catch {
    return false
  }
}

export function removeLocalDraft(problemId: string, language: SubmissionLanguage): void {
  try {
    localStorage.removeItem(key(problemId, language))
  } catch {
    // 지우지 못해도 다음에 다시 묻는 것뿐이다
  }
}
