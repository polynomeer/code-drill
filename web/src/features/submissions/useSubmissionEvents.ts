import { useEffect, useState } from 'react'
import { getSubmission, getTraceManifest } from '../../api/client'
import { subscribe } from '../../api/eventStream'
import type { TraceManifest } from '../replay/traceTypes'
import type { Submission } from '../../shared/types'

/**
 * 제출 상태 구독 (기술 설계서 §9.1).
 *
 * SSE 는 편의 채널이고 **최종 상태의 진실 원천은 DB 조회**다. 그래서 스트림이 끊기거나
 * 이벤트를 놓쳐도 UI 가 멈추지 않도록, 이벤트를 받을 때마다 조회로 수렴시킨다.
 *
 * 스트림에 인증이 필요해 `EventSource` 대신 fetch 로 읽는다 (§11.2). 브라우저의 자동
 * 재연결을 잃는 대신, 끊기면 조회로 수렴한다 — 원래 그것이 진실의 원천이다.
 */
export function useSubmissionEvents(submissionId: string | null) {
  const [submission, setSubmission] = useState<Submission | null>(null)
  const [trace, setTrace] = useState<TraceManifest | null>(null)

  useEffect(() => {
    setSubmission(null)
    setTrace(null)
    if (!submissionId) return

    let cancelled = false
    const refresh = () => {
      getSubmission(submissionId).then((next) => {
        if (!cancelled) setSubmission(next)
      })
      // 완료된 제출을 나중에 다시 열면 SSE 로는 trace 이벤트가 오지 않는다. 조회로 채운다.
      getTraceManifest(submissionId).then((next) => {
        if (!cancelled && next) setTrace(next)
      })
    }

    const close = subscribe(`/submissions/${submissionId}/events`, {
      onEvent: (name) => {
        if (name === 'trace') {
          getTraceManifest(submissionId).then((next) => {
            if (!cancelled) setTrace(next)
          })
        } else {
          refresh()
        }
      },
      // 스트림이 죽어도 결과를 보여줘야 한다. 조회가 진실의 원천이다.
      onError: refresh,
    })

    refresh()
    return () => {
      cancelled = true
      close()
    }
  }, [submissionId])

  return { submission, trace }
}
