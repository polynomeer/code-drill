/**
 * UX 이벤트 (디자인 설계서 §16.1, §16.2 계측 원칙).
 *
 * 이벤트와 속성의 표는 서버의 `EventSchema` 와 짝이다 — 한쪽만 바꾸면 서버가 그 속성을 버린다.
 * 타입이 표를 강제한다: 표에 없는 이벤트나 속성은 컴파일이 안 된다.
 *
 * - **소스·입력·출력 전문은 보내지 않는다.** 속성은 짧은 식별자·정해진 값·수뿐이다. 서버도 한 번 더 거른다.
 * - **누구인지 보내지 않는다.** 탭마다 바뀌는 세션 id 와 로그인 여부만.
 * - **같은 행동은 한 번.** 이벤트는 사용자가 한 일(누름·이동)에서 낸다 — 다시 그리기(effect)에서 내면 같은
 *   화면을 두 번 그릴 때 두 번 센다. 보기 이벤트처럼 effect 에서 낼 수밖에 없는 것은 [trackOnce] 로.
 *
 * 모아서 보낸다 — 2초 조용해지면, 열 개가 차면, 탭이 가려질 때. 보내기 실패는 조용히 버린다:
 * 계측이 화면을 막거나 오류를 내면 안 된다 (판정이 1차 흐름이다).
 */
import { getSession } from '../api/session'

type Lang = 'KOTLIN' | 'JAVA' | 'PYTHON'

export interface Events {
  problem_list_view: { filters: number; resultCount: number; sort: string }
  problem_open: { source: OpenSource; problemId: string }
  run_requested: { type: 'sample' | 'custom'; language: Lang }
  submission_created: { problem: string; language: Lang }
  verdict_viewed: { verdict: string; latency: number }
  replay_opened: { entry: 'drawer' | 'page' | 'link'; traceType: string }
  replay_seeked: { from: number; to: number; method: SeekMethod }
  code_trace_link_used: { direction: 'code-to-state' | 'state-to-code'; eventType: string }
  post_ac_action: { action: 'replay' | 'editorial' | 'next' | 'compare' }
}

export type OpenSource = 'list' | 'search' | 'prescription' | 'contest' | 'link' | 'direct'
export type SeekMethod = 'scrub' | 'key' | 'button' | 'line' | 'marker' | 'divergence' | 'list'

const ENDPOINT = '/api/v1/events'
const FLUSH_DELAY_MS = 2000
const BATCH = 10
/** 서버와 같은 한도. 넘는 것은 서버가 버리므로 여기서도 자른다. */
const MAX_STRING = 64

/** 배포마다 바뀌는 값 — 실험·회귀를 "어느 화면이었나"로 가를 때 쓴다 (§16.2). */
export const UI_VERSION = 'u9'

const queue: { name: string; props: Record<string, unknown> }[] = []
let timer: ReturnType<typeof setTimeout> | undefined
const once = new Set<string>()

function sessionId(): string {
  try {
    let id = sessionStorage.getItem('codedrill.analytics-session')
    if (!id) {
      id = crypto.randomUUID()
      sessionStorage.setItem('codedrill.analytics-session', id)
    }
    return id
  } catch {
    return 'no-storage-session'
  }
}

export function track<E extends keyof Events>(name: E, props: Events[E]): void {
  const safe: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(props)) {
    // 문자열이 길면 무엇인가 잘못 들어온 것이다 — 보내지 않는다
    if (typeof value === 'string' && value.length > MAX_STRING) continue
    safe[key] = value
  }
  queue.push({ name, props: safe })
  if (queue.length >= BATCH) flush()
  else {
    clearTimeout(timer)
    timer = setTimeout(flush, FLUSH_DELAY_MS)
  }
}

/** 같은 열쇠로는 이 탭에서 한 번만. 보기 이벤트(목록을 봤다)처럼 effect 에서 낼 때 쓴다. */
export function trackOnce<E extends keyof Events>(key: string, name: E, props: Events[E]): void {
  if (once.has(key)) return
  once.add(key)
  track(name, props)
}

export function flush(): void {
  clearTimeout(timer)
  if (queue.length === 0) return
  const body = JSON.stringify({ session: sessionId(), uiVersion: UI_VERSION, events: queue.splice(0) })
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  // 로그인했다는 사실만 서버가 알게 — 서버는 토큰으로 누구인지 알 수 있지만 쓰지 않는다 (EventController)
  const session = getSession()
  if (session) headers.Authorization = `Bearer ${session.accessToken}`
  void fetch(ENDPOINT, { method: 'POST', headers, body, keepalive: true }).catch(() => undefined)
}

/* ─── 문제를 어디서 열었나 (problem_open.source) ─── */

const OPEN_SOURCE = 'codedrill.open-source'

/** 문제로 가는 링크를 누를 때 남긴다. 풀이 화면이 열리며 읽고 지운다. */
export function markOpenSource(source: OpenSource): void {
  try {
    sessionStorage.setItem(OPEN_SOURCE, source)
  } catch {
    // 출처를 모르면 direct 로 센다
  }
}

export function takeOpenSource(): OpenSource {
  try {
    const value = sessionStorage.getItem(OPEN_SOURCE) as OpenSource | null
    sessionStorage.removeItem(OPEN_SOURCE)
    return value ?? 'direct'
  } catch {
    return 'direct'
  }
}

if (typeof window !== 'undefined') {
  // 탭이 가려지거나 페이지를 떠나면(새로고침·다른 주소) 남은 것을 보낸다 — keepalive 라 떠난 뒤에도 간다
  window.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') flush()
  })
  window.addEventListener('pagehide', flush)
}
