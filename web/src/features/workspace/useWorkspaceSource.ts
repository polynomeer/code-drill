import { useEffect, useState } from 'react'
import { getDraft } from '../../api/client'
import type { Problem, SubmissionLanguage } from '../../shared/types'
import { starterFor } from './starters'
import { functionName } from './statement'
import { useAutoSave } from './useAutoSave'

/**
 * 편집기에 담길 코드와 그 저장 (기술 설계서 §9.2).
 *
 * 저장된 초안이 있으면 그것이 먼저다 (§0.1 복구 가능). 없으면 언어별 시작 코드를 준다.
 * 사용자가 이미 손댄 뒤에는 어느 쪽도 덮어쓰지 않는다.
 */
export function useWorkspaceSource(problem: Problem | null, language: SubmissionLanguage) {
  const [source, setSource] = useState('')
  const [touched, setTouched] = useState(false)
  const autoSave = useAutoSave(problem?.id ?? null, language, source, touched)

  // 언어를 바꾸면 그 언어의 초안을 새로 읽는다. 언어별 초안은 따로 보존된다 (§6.3).
  useEffect(() => setTouched(false), [language, problem?.id])

  useEffect(() => {
    if (!problem || touched) return
    let cancelled = false
    const starter = () => starterFor(language, problem.signature, functionName(problem.signature))
    getDraft(problem.id, language)
      .then((draft) => {
        if (!cancelled) setSource(draft?.code ?? starter())
      })
      .catch(() => {
        if (!cancelled) setSource(starter())
      })
    return () => {
      cancelled = true
    }
  }, [problem, language, touched])

  return {
    source,
    saveState: autoSave.state,
    edit: (next: string) => {
      setTouched(true)
      setSource(next)
    },
    /** 시작 코드로 되돌린다. 손댄 것으로 치므로 자동 저장이 초안을 덮어쓴다. */
    reset: () => {
      if (!problem) return
      setTouched(true)
      setSource(starterFor(language, problem.signature, functionName(problem.signature)))
    },
    /** code 가 null 이면 편집 중인 내용을 유지하고 그대로 덮어쓴다. */
    resolveConflict: (code: string | null, version: number) => {
      if (code !== null) {
        // 서버 것 가져오기: 편집기를 서버 내용으로 맞추고 저장은 하지 않는다.
        setSource(code)
        autoSave.resolveConflict(version)
      } else {
        autoSave.resolveConflict(version, source)
      }
    },
  }
}
