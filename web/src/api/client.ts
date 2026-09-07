import { getSession, refreshSession, setSession, type Session } from './session'
import type { TraceChunk, TraceManifest } from '../features/replay/traceTypes'
import type {
  ApiError,
  Draft,
  DraftConflict,
  Page,
  Problem,
  ProblemSummary,
  Submission,
  SubmissionLanguage,
} from '../shared/types'

const BASE = '/api/v1'

export class ApiFailure extends Error {
  constructor(
    readonly status: number,
    readonly detail: ApiError,
  ) {
    super(detail.message)
    this.name = 'ApiFailure'
  }
}

async function json<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
  return (await response.json()) as T
}

/**
 * 인증이 필요한 요청 (기술 설계서 §11.2).
 *
 * access token 이 만료되면 **한 번** 갱신하고 같은 요청을 다시 보낸다. 화면 어디서도
 * 만료를 다루지 않게 하려는 것이며, 재시도를 한 번으로 제한해 갱신이 계속 실패할 때
 * 무한 루프에 빠지지 않게 한다.
 */
export async function authed(path: string, init: RequestInit = {}): Promise<Response> {
  const send = (token: string) =>
    fetch(`${BASE}${path}`, {
      ...init,
      headers: { ...(init.headers ?? {}), Authorization: `Bearer ${token}` },
    })

  const session = getSession()
  if (!session) throw new ApiFailure(401, {
    errorCode: 'UNAUTHENTICATED',
    message: '로그인이 필요하다',
    traceId: '',
  } as ApiError)

  const first = await send(session.accessToken)
  if (first.status !== 401) return first

  const renewed = await refreshSession()
  if (!renewed) return first
  return send(renewed.accessToken)
}

// --- 인증 (§9.2) ---

export async function register(email: string, displayName: string, password: string): Promise<Session> {
  const response = await fetch(`${BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, displayName, password }),
  })
  const session = await json<Session>(response)
  setSession(session)
  return session
}

export async function login(email: string, password: string): Promise<Session> {
  const response = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const session = await json<Session>(response)
  setSession(session)
  return session
}

/** 로그아웃. 서버 호출이 실패해도 이 브라우저의 세션은 지운다. */
export async function logout(): Promise<void> {
  await authed('/auth/logout', { method: 'POST' }).catch(() => undefined)
  setSession(null)
}

export function listProblems(query?: string): Promise<Page<ProblemSummary>> {
  const params = query ? `?query=${encodeURIComponent(query)}` : ''
  return fetch(`${BASE}/problems${params}`).then(json<Page<ProblemSummary>>)
}

export function listSubmissions(problemId?: string): Promise<Page<Submission>> {
  const params = problemId ? `?problemId=${encodeURIComponent(problemId)}` : ''
  return authed(`/submissions${params}`).then(json<Page<Submission>>)
}

export async function getDraft(problemId: string, language: string): Promise<Draft | null> {
  const response = await authed(`/workspaces/${problemId}/${language}`)
  if (response.status === 204) return null
  return json<Draft>(response)
}

/**
 * 초안 저장 (§9.2 PUT /workspaces).
 *
 * 409 는 오류가 아니라 결과의 한 종류다. 그 사이에 다른 곳에서 저장됐다는 뜻이고,
 * 서버의 현재 초안이 함께 온다. 조용히 덮어쓰지 않는 것이 이 API 의 목적이다.
 */
export async function saveDraft(
  problemId: string,
  language: string,
  code: string,
  version: number | null,
): Promise<{ saved: true; version: number } | { saved: false; conflict: DraftConflict }> {
  const response = await authed(`/workspaces/${problemId}/${language}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, version }),
  })
  if (response.status === 409) {
    return { saved: false, conflict: (await response.json()) as DraftConflict }
  }
  const body = await json<{ version: number }>(response)
  return { saved: true, version: body.version }
}

export function getProblem(slug: string): Promise<Problem> {
  return fetch(`${BASE}/problems/${slug}`).then(json<Problem>)
}

export function getSubmission(id: string): Promise<Submission> {
  return authed(`/submissions/${id}`).then(json<Submission>)
}

/**
 * 트레이스 목차. 판정과 독립이라 아직 없을 수 있고, 없는 것은 오류가 아니므로 null 이다.
 */
export async function getTraceManifest(id: string): Promise<TraceManifest | null> {
  const response = await authed(`/submissions/${id}/trace`)
  if (response.status === 204) return null
  return json<TraceManifest>(response)
}

/** 이벤트 청크. 현재 위치 주변만 내려받는다 (§7.5). */
export function getTraceChunk(id: string, index: number): Promise<TraceChunk> {
  return authed(`/submissions/${id}/trace/chunks/${index}`).then(json<TraceChunk>)
}

/**
 * 제출 생성 (기술 설계서 §9.1).
 *
 * Idempotency-Key 는 선택이 아니다. 버튼 중복 클릭이나 네트워크 재시도가 제출을 두 번
 * 만들지 않게 하는 유일한 장치라서, 호출부가 빠뜨릴 수 없도록 여기서 항상 붙인다.
 */
export function createSubmission(
  problemId: string,
  problemVersion: number,
  language: SubmissionLanguage,
  source: string,
) {
  return authed('/submissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': crypto.randomUUID(),
    },
    body: JSON.stringify({ problemId, problemVersion, language, source }),
  }).then(json<Submission>)
}
