import { useEffect, useState } from 'react'
import type { SubmissionStatus } from '../../shared/types'

/**
 * 제출 상태 SSE 구독 (기술 설계서 §9.1).
 *
 * SSE 는 편의 채널이고 **최종 상태의 진실 원천은 DB 조회**다. 그래서 스트림이 끊기거나
 * 이벤트가 유실돼도 UI 가 멈추지 않도록, 종료 상태에 도달하기 전에는 재조회로 수렴시킨다.
 * 재연결은 브라우저가 Last-Event-ID 로 처리한다.
 */
export function useSubmissionEvents(submissionId: string | null) {
  const [status, setStatus] = useState<SubmissionStatus | null>(null)

  useEffect(() => {
    if (!submissionId) return

    const source = new EventSource(`/api/v1/submissions/${submissionId}/events`)
    source.addEventListener('status', (event) => {
      const payload = JSON.parse((event as MessageEvent<string>).data) as {
        status: SubmissionStatus
      }
      setStatus(payload.status)
    })

    return () => source.close()
  }, [submissionId])

  return status
}
