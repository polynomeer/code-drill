import { useCallback, useEffect, useRef, useState } from 'react'

/** [C] 는 충돌 때 서버가 실어 보내는 현재 초안의 모양이다. 화면이 그것을 그대로 보여 준다. */
export type DraftSyncState<C> =
  | { status: 'idle' }
  | { status: 'saving' }
  | { status: 'saved'; at: number }
  | { status: 'conflict'; current: C }
  | { status: 'failed'; message: string }

export type DraftSaveResult<C> = { saved: true; version: number } | { saved: false; current: C }

/**
 * 초안 자동 저장의 뼈대 (기술 설계서 §9.2, 디자인 설계서 §0.1 복구 가능).
 *
 * 무엇을 저장하는지는 모른다 — 소스 한 문자열이든 파일 여럿이든 [load]·[save] 가 정한다.
 * 여기 있는 것은 그 둘에 공통인 규칙이다: 타이핑마다 저장하지 않고 [DEBOUNCE_MS] 만큼
 * 잠잠해지면 보내고, 한 번에 하나만 날리며, 그 사이에 바뀐 내용은 끝나고 다시 보낸다.
 *
 * 충돌(409)은 실패가 아니다. 다른 탭이 먼저 저장했다는 뜻이므로 **자동으로 덮어쓰지
 * 않고** 사용자에게 선택을 넘긴다. 여기서 조용히 이기면 작성 중이던 것이 사라진다.
 */
export function useDraftSync<T, C>({
  key,
  value,
  enabled,
  load,
  save,
}: {
  /** 초안의 정체. 바뀌면 다른 초안이라 버전을 이어 쓰지 않는다. */
  key: string | null
  value: T
  enabled: boolean
  load: () => Promise<{ version: number } | null>
  save: (value: T, version: number | null) => Promise<DraftSaveResult<C>>
}) {
  const [state, setState] = useState<DraftSyncState<C>>({ status: 'idle' })
  const versionRef = useRef<number | null>(null)
  const inFlightRef = useRef(false)
  const pendingRef = useRef<T | null>(null)
  const saveRef = useRef(save)
  saveRef.current = save

  useEffect(() => {
    versionRef.current = null
    setState({ status: 'idle' })
    if (!key) return
    load()
      .then((draft) => {
        versionRef.current = draft?.version ?? null
      })
      .catch(() => {
        // 초안을 못 읽어도 편집은 계속돼야 한다. 첫 저장이 충돌로 알려줄 것이다.
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const flush = useCallback(
    async (next: T) => {
      if (!key) return
      if (inFlightRef.current) {
        pendingRef.current = next
        return
      }
      inFlightRef.current = true
      setState({ status: 'saving' })
      try {
        const result = await saveRef.current(next, versionRef.current)
        if (result.saved) {
          versionRef.current = result.version
          setState({ status: 'saved', at: Date.now() })
        } else {
          setState({ status: 'conflict', current: result.current })
        }
      } catch (error) {
        setState({ status: 'failed', message: error instanceof Error ? error.message : '저장에 실패했습니다' })
      } finally {
        inFlightRef.current = false
        const queued = pendingRef.current
        pendingRef.current = null
        if (queued !== null && queued !== next) void flush(queued)
      }
    },
    [key],
  )

  useEffect(() => {
    if (!enabled || !key) return
    const timer = setTimeout(() => void flush(value), DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [value, enabled, key, flush])

  /**
   * 충돌을 사용자가 정리했다. 서버 버전을 기준으로 삼는다.
   *
   * [overwriteWith] 가 있으면 그 내용으로 **즉시 저장한다.** "내 것 유지하고 덮어쓰기"는
   * 버튼 문구 그대로 덮어써야 한다. 버전만 맞추고 다음 타이핑을 기다리면, 사용자는
   * 덮어썼다고 믿는데 서버에는 남의 것이 남아 있다.
   */
  const resolveConflict = useCallback(
    (version: number, overwriteWith?: T) => {
      versionRef.current = version
      setState({ status: 'idle' })
      if (overwriteWith !== undefined) void flush(overwriteWith)
    },
    [flush],
  )

  /** 초안을 버렸다. 다음 저장은 "아직 초안이 없다"는 주장으로 나간다. */
  const reset = useCallback(() => {
    versionRef.current = null
    pendingRef.current = null
    setState({ status: 'idle' })
  }, [])

  return { state, resolveConflict, reset }
}

const DEBOUNCE_MS = 1200
