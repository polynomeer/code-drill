import { useEffect, useRef, useState } from 'react'
import { getDraft } from '../../api/client'
import type { Problem, SubmissionLanguage } from '../../shared/types'
import { readLocalDraft, removeLocalDraft, writeLocalDraft } from './localDraft'
import { starterFor } from './starters'
import { functionName } from './statement'
import { useAutoSave } from './useAutoSave'

/**
 * 편집기에 담길 코드와 그 저장 (기술 설계서 §9.2).
 *
 * 저장된 초안이 있으면 그것이 먼저다 (§0.1 복구 가능). 없으면 언어별 시작 코드를 준다.
 * 사용자가 이미 손댄 뒤에는 어느 쪽도 덮어쓰지 않는다.
 *
 * **로그인 전에는 이 기기에만 저장한다** (localDraft.ts). 로그인한 뒤 같은 문제를 열면 그 초안을
 * 서버 초안과 견준다 — 서버 초안이 없거나 같으면 그대로 이어 쓰고(잃을 것이 없다), 다르면
 * [localDraft] 로 내놓아 화면이 어느 쪽으로 갈지 묻게 한다. 조용히 한쪽을 고르면 다른 쪽을 잃는다.
 */
export function useWorkspaceSource(problem: Problem | null, language: SubmissionLanguage, signedIn = true) {
  const [source, setSource] = useState('')
  const [touched, setTouched] = useState(false)
  /** 로그인 뒤 서버 초안과 다른 로그인 전 초안. 있으면 화면이 고르게 한다. */
  const [localDraft, setLocalDraft] = useState<string | null>(null)
  /** 로그인 전 초안을 서버 초안으로 이어받았다 — 화면이 한 번 알린다. */
  const [adopted, setAdopted] = useState(false)
  const [localSaved, setLocalSaved] = useState<boolean | null>(null)
  const autoSave = useAutoSave(signedIn ? (problem?.id ?? null) : null, language, source, touched && signedIn)
  const localTimer = useRef<number | undefined>(undefined)

  // 언어를 바꾸면 그 언어의 초안을 새로 읽는다. 언어별 초안은 따로 보존된다 (§6.3).
  useEffect(() => {
    setTouched(false)
    setLocalDraft(null)
  }, [language, problem?.id, signedIn])

  useEffect(() => {
    if (!problem || touched) return
    let cancelled = false
    const starter = () => starterFor(language, problem.signature, functionName(problem.signature))
    const local = readLocalDraft(problem.id, language)

    if (!signedIn) {
      setSource(local ?? starter())
      return
    }

    getDraft(problem.id, language)
      .then((draft) => {
        if (cancelled) return
        if (local === null || local === draft?.code) {
          if (local !== null) removeLocalDraft(problem.id, language)
          setSource(draft?.code ?? starter())
        } else if (!draft) {
          // 서버에 초안이 없다 — 잃을 것이 없으니 이어 쓴다. 손댄 것으로 쳐 자동 저장이 서버로 올린다
          setSource(local)
          setTouched(true)
          setAdopted(true)
          removeLocalDraft(problem.id, language)
        } else {
          setSource(draft.code)
          setLocalDraft(local)
        }
      })
      .catch(() => {
        if (!cancelled) setSource(local ?? starter())
      })
    return () => {
      cancelled = true
    }
  }, [problem, language, touched, signedIn])

  const edit = (next: string) => {
    setTouched(true)
    setSource(next)
    if (!signedIn && problem) {
      window.clearTimeout(localTimer.current)
      const id = problem.id
      localTimer.current = window.setTimeout(() => setLocalSaved(writeLocalDraft(id, language, next)), 300)
    }
  }

  return {
    source,
    saveState: autoSave.state,
    /** 로그인 전: 이 기기에 적었나 (null 이면 아직 손대지 않았다) */
    localSaved,
    localDraft,
    adopted,
    edit,
    /** 로그인 전 초안으로 간다. 서버 초안은 자동 저장이 덮어쓴다. */
    takeLocalDraft: () => {
      if (!problem || localDraft === null) return
      setSource(localDraft)
      setTouched(true)
      setLocalDraft(null)
      removeLocalDraft(problem.id, language)
    },
    /** 저장된 초안을 지킨다. 로그인 전 초안은 버린다. */
    keepServerDraft: () => {
      if (!problem) return
      setLocalDraft(null)
      removeLocalDraft(problem.id, language)
    },
    /** 시작 코드로 되돌린다. 손댄 것으로 치므로 자동 저장이 초안을 덮어쓴다. */
    reset: () => {
      if (!problem) return
      edit(starterFor(language, problem.signature, functionName(problem.signature)))
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
