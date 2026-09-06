import { useCallback, useEffect, useRef, useState } from 'react'
import { getDraft, saveDraft } from '../../api/client'
import type { Draft, SubmissionLanguage } from '../../shared/types'

export type SaveState =
  | { status: 'idle' }
  | { status: 'saving' }
  | { status: 'saved'; at: number }
  | { status: 'conflict'; current: Draft }
  | { status: 'failed'; message: string }

/**
 * 초안 자동 저장 (기술 설계서 §9.2, 디자인 설계서 §0.1 복구 가능).
 *
 * 타이핑마다 저장하지 않고 [DEBOUNCE_MS] 만큼 잠잠해지면 보낸다. 저장 요청이 겹치지
 * 않도록 한 번에 하나만 날리고, 그 사이에 바뀐 내용은 끝나고 다시 보낸다.
 *
 * 충돌(409)은 실패가 아니다. 다른 탭이 먼저 저장했다는 뜻이므로 **자동으로 덮어쓰지
 * 않고** 사용자에게 선택을 넘긴다. 여기서 조용히 이기면 작성 중이던 코드가 사라진다.
 */
export function useAutoSave(
  problemId: string | null,
  language: SubmissionLanguage,
  code: string,
  enabled: boolean,
) {
  const [state, setState] = useState<SaveState>({ status: 'idle' })
  const versionRef = useRef<number | null>(null)
  const inFlightRef = useRef(false)
  const pendingRef = useRef<string | null>(null)

  // 문제나 언어가 바뀌면 다른 초안이다. 버전을 이어 쓰면 남의 초안을 덮어쓰게 된다.
  useEffect(() => {
    versionRef.current = null
    setState({ status: 'idle' })
    if (!problemId) return
    getDraft(problemId, language)
      .then((draft) => {
        versionRef.current = draft?.version ?? null
      })
      .catch(() => {
        // 초안을 못 읽어도 편집은 계속돼야 한다. 첫 저장이 충돌로 알려줄 것이다.
      })
  }, [problemId, language])

  const flush = useCallback(
    async (next: string) => {
      if (!problemId) return
      if (inFlightRef.current) {
        pendingRef.current = next
        return
      }
      inFlightRef.current = true
      setState({ status: 'saving' })
      try {
        const result = await saveDraft(problemId, language, next, versionRef.current)
        if (result.saved) {
          versionRef.current = result.version
          setState({ status: 'saved', at: Date.now() })
        } else {
          setState({ status: 'conflict', current: result.conflict.current })
        }
      } catch (error) {
        setState({
          status: 'failed',
          message: error instanceof Error ? error.message : '저장에 실패했습니다',
        })
      } finally {
        inFlightRef.current = false
        const queued = pendingRef.current
        pendingRef.current = null
        if (queued !== null && queued !== next) void flush(queued)
      }
    },
    [problemId, language],
  )

  useEffect(() => {
    if (!enabled || !problemId) return
    const timer = setTimeout(() => void flush(code), DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [code, enabled, problemId, flush])

  /**
   * 충돌을 사용자가 정리했다. 서버 버전을 기준으로 삼는다.
   *
   * [overwriteWith] 가 있으면 그 내용으로 **즉시 저장한다.** "내 것 유지하고 덮어쓰기"는
   * 버튼 문구 그대로 덮어써야 한다. 버전만 맞추고 다음 타이핑을 기다리면, 사용자는
   * 덮어썼다고 믿는데 서버에는 남의 코드가 남아 있다.
   */
  const resolveConflict = useCallback(
    (version: number, overwriteWith?: string) => {
      versionRef.current = version
      setState({ status: 'idle' })
      if (overwriteWith !== undefined) void flush(overwriteWith)
    },
    [flush],
  )

  return { state, resolveConflict }
}

const DEBOUNCE_MS = 1200
