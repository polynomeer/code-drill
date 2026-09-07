/**
 * 로그인 세션 (기술 설계서 §11.2).
 *
 * access token 은 짧고 refresh token 은 회전한다. 화면 어디서도 만료를 신경 쓰지
 * 않도록, 갱신은 이 모듈이 요청 실패를 보고 알아서 한다.
 *
 * `localStorage` 에 둔다. 새로고침으로 로그인이 풀리면 코드를 쓰다 말고 다시 들어와야
 * 하기 때문이다. XSS 가 나면 읽히지만, 그 경우 메모리에 둔 토큰도 같은 스크립트가
 * 읽는다 — 이 선택으로 막을 수 있는 것은 없고 잃는 것만 있다.
 */
const KEY = 'codedrill.session'

export type Session = {
  accessToken: string
  refreshToken: string
  userId: string
  displayName: string
}

let current: Session | null = read()
const listeners = new Set<(session: Session | null) => void>()

function read(): Session | null {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    // 저장소를 못 읽는 브라우저 설정이 있다. 로그인만 다시 하면 되므로 치명적이지 않다.
    return null
  }
}

export function getSession(): Session | null {
  return current
}

export function setSession(session: Session | null) {
  current = session
  try {
    if (session) localStorage.setItem(KEY, JSON.stringify(session))
    else localStorage.removeItem(KEY)
  } catch {
    // 저장에 실패해도 이번 탭에서는 계속 쓸 수 있다.
  }
  listeners.forEach((listener) => listener(session))
}

export function onSessionChange(listener: (session: Session | null) => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/**
 * access token 을 갱신한다.
 *
 * **동시에 여러 요청이 만료를 만나도 갱신은 한 번만 한다.** refresh 는 회전하므로,
 * 두 요청이 같은 refresh 를 쓰면 뒤늦은 쪽이 "이미 쓴 토큰"이 되어 서버가 세션을
 * 통째로 끊는다 — 여러 탭에서 동시에 제출할 때 흔히 생긴다.
 */
let refreshing: Promise<Session | null> | null = null

export function refreshSession(): Promise<Session | null> {
  if (refreshing) return refreshing

  const session = current
  if (!session) return Promise.resolve(null)

  refreshing = fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        setSession(null)
        return null
      }
      const body = (await response.json()) as Session
      setSession(body)
      return body
    })
    .catch(() => {
      // 네트워크 실패는 로그아웃이 아니다. 토큰을 지우면 잠깐 끊긴 것으로 로그인이 풀린다.
      return current
    })
    .finally(() => {
      refreshing = null
    })

  return refreshing
}
