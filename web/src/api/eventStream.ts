import { authed } from './client'

/**
 * SSE 구독 (기술 설계서 §9.1, §11.2).
 *
 * `EventSource` 를 쓰지 않는다. 헤더를 붙일 수 없어 토큰을 쿼리 문자열로 보내야 하는데,
 * URL 은 프록시 로그·브라우저 기록·리퍼러에 그대로 남아 토큰이 우리가 통제하지 않는
 * 곳에 복제된다 (§11.3). 대신 fetch 로 스트림을 직접 읽는다.
 *
 * 잃는 것은 브라우저의 자동 재연결이다. 그래서 [onError] 를 부르고, 호출부는 조회로
 * 수렴한다 — SSE 는 편의 채널이고 최종 상태의 진실 원천은 조회다.
 */
export function subscribe(
  path: string,
  handlers: { onEvent: (name: string, data: string) => void; onError: () => void },
): () => void {
  const controller = new AbortController()

  authed(path, { signal: controller.signal, headers: { Accept: 'text/event-stream' } })
    .then(async (response) => {
      if (!response.ok || !response.body) {
        handlers.onError()
        return
      }

      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
      // SSE 는 빈 줄로 이벤트를 끊는다. 청크 경계는 그것과 무관하므로 버퍼에 모아
      // 두었다가 끊긴 만큼만 꺼낸다 — 청크마다 파싱하면 반쪽짜리 이벤트를 읽는다.
      let buffer = ''

      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += value

        let boundary = buffer.indexOf('\n\n')
        while (boundary !== -1) {
          emit(buffer.slice(0, boundary), handlers.onEvent)
          buffer = buffer.slice(boundary + 2)
          boundary = buffer.indexOf('\n\n')
        }
      }
    })
    .catch(() => {
      // 취소는 오류가 아니다. 화면을 떠날 때마다 오류를 띄우면 안 된다.
      if (!controller.signal.aborted) handlers.onError()
    })

  return () => controller.abort()
}

function emit(block: string, onEvent: (name: string, data: string) => void) {
  let name = 'message'
  const data: string[] = []

  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) name = line.slice(6).trim()
    else if (line.startsWith('data:')) data.push(line.slice(5).trim())
  }
  if (data.length > 0) onEvent(name, data.join('\n'))
}
