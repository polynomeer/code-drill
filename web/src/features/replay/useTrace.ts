import { useCallback, useEffect, useRef, useState } from 'react'
import { getTraceChunk } from '../../api/client'
import type { TargetKind, TraceEvent, TraceManifest } from './traceTypes'

/**
 * 트레이스 청크 로더 (기술 설계서 §7.5).
 *
 * manifest 와 요약을 먼저 받고, 상세 이벤트는 **현재 위치 주변 청크만** 내려받는다.
 * 20,000 이벤트짜리 트레이스를 통째로 받으면 리플레이를 열기까지가 느려지고, 대부분은
 * 끝까지 보지 않는다.
 *
 * 리듀서는 "앞에서부터 N개 적용"이라 임의 위치로 가려면 그 앞의 청크도 필요하다. 그래서
 * 위치가 뒤로 갈수록 필요한 청크가 누적된다. 앞 청크를 버리지 않고 들고 있는 이유다.
 */
export function useTrace(submissionId: string | null, manifest: TraceManifest | null) {
  const [events, setEvents] = useState<TraceEvent[]>([])
  const [loadedUpTo, setLoadedUpTo] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const loading = useRef(new Set<number>())

  useEffect(() => {
    setEvents([])
    setLoadedUpTo(0)
    setError(null)
    loading.current.clear()
  }, [submissionId, manifest?.traceId])

  /** [position] 까지 재생하는 데 필요한 청크를 모두 확보한다. */
  const ensureLoaded = useCallback(
    async (position: number) => {
      if (!submissionId || !manifest) return
      const needed = manifest.chunks.filter(
        (chunk) => chunk.index * chunkSize(manifest) < position + PREFETCH_EVENTS,
      )

      for (const chunk of needed) {
        if (chunk.index < loadedChunkCount(events, manifest)) continue
        if (loading.current.has(chunk.index)) continue
        loading.current.add(chunk.index)
        try {
          const loaded = await getTraceChunk(submissionId, chunk.index)
          setEvents((current) => {
            // 청크는 순서대로 이어 붙인다. 건너뛴 청크가 있으면 상태가 어긋나므로
            // 다음 청크만 받아들인다.
            if (loaded.index !== loadedChunkCount(current, manifest)) return current
            return [...current, ...loaded.events]
          })
          setLoadedUpTo((current) => Math.max(current, (chunk.index + 1) * chunkSize(manifest)))
        } catch {
          // 청크 하나를 못 받아도 지금까지 받은 만큼은 재생할 수 있어야 한다 (§7.5 폴백).
          setError('일부 구간을 불러오지 못했습니다. 받은 데까지만 재생합니다.')
        } finally {
          loading.current.delete(chunk.index)
        }
      }
    },
    [submissionId, manifest, events],
  )

  useEffect(() => {
    void ensureLoaded(0)
    // 최초 진입 시 첫 청크만 확보한다. 나머지는 위치가 움직일 때 따라온다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [submissionId, manifest?.traceId])

  return { events, loadedUpTo, error, ensureLoaded }
}

/** 트레이스에 실제로 등장한 자료구조 종류. 렌더러를 고르는 근거다. */
export function kindsIn(events: TraceEvent[], manifest: TraceManifest | null): Set<TargetKind> {
  const kinds = new Set<TargetKind>()
  for (const event of events) kinds.add(event.targetKind)
  // 아직 청크를 못 받았어도 요약만으로 어떤 렌더러가 필요한지 알 수 있다.
  for (const event of manifest?.summary ?? []) kinds.add(event.targetKind)
  return kinds
}

function chunkSize(manifest: TraceManifest): number {
  return manifest.chunks[0]?.eventCount ?? 1
}

function loadedChunkCount(events: TraceEvent[], manifest: TraceManifest): number {
  const size = chunkSize(manifest)
  return size > 0 ? Math.ceil(events.length / size) : 0
}

/** 현재 위치보다 이만큼 앞까지 미리 받아 둔다. */
const PREFETCH_EVENTS = 500
