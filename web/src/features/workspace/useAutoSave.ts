import { useCallback } from 'react'
import { getDraft, saveDraft } from '../../api/client'
import type { Draft, SubmissionLanguage } from '../../shared/types'
import { useDraftSync, type DraftSyncState } from './useDraftSync'

export type SaveState = DraftSyncState<Draft>

/**
 * 알고리즘 문제의 초안 자동 저장 — 소스 한 문자열 (기술 설계서 §9.2).
 *
 * 규칙은 [useDraftSync] 에 있다. 여기는 무엇을 읽고 쓰는지만 안다.
 */
export function useAutoSave(
  problemId: string | null,
  language: SubmissionLanguage,
  code: string,
  enabled: boolean,
) {
  const key = problemId ? `${problemId}/${language}` : null
  const load = useCallback(() => (problemId ? getDraft(problemId, language) : Promise.resolve(null)), [problemId, language])
  const save = useCallback(
    async (next: string, version: number | null) => {
      const result = await saveDraft(problemId!, language, next, version)
      return result.saved ? result : { saved: false as const, current: result.conflict.current }
    },
    [problemId, language],
  )
  return useDraftSync<string, Draft>({ key, value: code, enabled, load, save })
}
