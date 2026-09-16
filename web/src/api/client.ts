import { getSession, refreshSession, setSession, type Session } from './session'
import type { TraceChunk, TraceManifest } from '../features/replay/traceTypes'
import type {
  ApiError,
  ContestSummary,
  ContestView,
  Contributions,
  Rating,
  SanctionView,
  DiscussionAnchorRequest,
  DiscussionPost,
  DiscussionThread,
  Draft,
  DraftConflict,
  Page,
  AnsweredQuestion,
  CompetencyMap,
  EvidenceView,
  PreQuestionSet,
  Problem,
  ProblemFilter,
  ProblemPage,
  Submission,
  QuestionKind,
  SubmissionLanguage,
  ArenaAttempt,
  ArenaBoard,
  ArenaDonation,
  CoachingSession,
  Collection,
  Prescription,
  Stats,
  WeeklyReport,
  Counterexample,
  Divergence,
  Editorial,
  LabRun,
  StatePrediction,
  MutationCheck,
  TransferTask,
  Trial,
  TrialCaseInput,
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

/**
 * 시험 실행을 시작한다 (기획서 부록 A 실행 도메인).
 *
 * 제출과 다른 경로다 — 판정이 되지 않고 제출 기록에도 남지 않는다.
 */
export async function startTrial(
  problemId: string,
  language: SubmissionLanguage,
  source: string,
  cases: TrialCaseInput[],
): Promise<Trial> {
  const response = await authed('/trials', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId, language, source, cases }),
  })
  return json<Trial>(response)
}

/**
 * 결과를 가져온다.
 *
 * 폴링이다. 제출은 SSE 로 미는데, 시험 실행은 몇 초면 끝나고 화면 하나만 보고 있으므로
 * 연결을 하나 더 여는 값을 하지 못한다.
 */
export async function getTrial(id: string): Promise<Trial> {
  return json<Trial>(await authed(`/trials/${id}`))
}

/**
 * 코칭 세션을 연다 (PRD FR-802).
 *
 * 이미 열려 있으면 그것을 돌려준다 — 문제를 다시 열 때마다 새 세션이 생기면 "이 문제에서
 * 몇 단계까지 봤나"가 세션마다 흩어진다.
 */
export async function openCoaching(problemId: string): Promise<CoachingSession> {
  const response = await authed('/coaching/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId }),
  })
  return json<CoachingSession>(response)
}

/** 다음 단계를 펼친다. 본문은 이 응답에만 실려 온다. */
export async function revealHint(
  sessionId: string,
  competency: string,
): Promise<CoachingSession> {
  const response = await authed(`/coaching/sessions/${sessionId}/reveal`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ competency }),
  })
  return json<CoachingSession>(response)
}

/**
 * 다음 이벤트를 맞혀 본다 (PRD FR-805).
 *
 * 자리는 화면의 위치가 아니라 **이벤트의 seq** 로 보낸다. 위치는 몇 개를 불러왔는지에
 * 달려 있어 서버와 어긋날 수 있다.
 */
export async function predictNext(
  submissionId: string,
  seq: number,
  predicted: string,
  rationale: string | null,
): Promise<StatePrediction> {
  const response = await authed(`/submissions/${submissionId}/predictions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ seq, predicted, rationale }),
  })
  return json<StatePrediction>(response)
}

/** 이미 맞혀 본 자리들. 같은 자리를 다시 묻지 않기 위해서다. */
export async function getPredictions(submissionId: string): Promise<StatePrediction[]> {
  return json<StatePrediction[]>(await authed(`/submissions/${submissionId}/predictions`))
}

/**
 * 최소 반례 축소를 건다 (기술 설계서 §6.3).
 *
 * 이 시스템에서 가장 오래 걸리는 작업이다. 사용자가 눌러야 돌고, 같은 제출에 두 번
 * 눌러도 한 번만 돈다.
 */
export async function startCounterexample(submissionId: string): Promise<Counterexample> {
  const response = await authed(`/submissions/${submissionId}/counterexample`, { method: 'POST' })
  return json<Counterexample>(response)
}

/** 축소 결과. 아직 걸지 않았으면 null 이다. */
export async function getCounterexample(submissionId: string): Promise<Counterexample | null> {
  const response = await authed(`/submissions/${submissionId}/counterexample`)
  if (response.status === 204) return null
  return json<Counterexample>(response)
}

/** 오늘의 처방 (PRD FR-808). */
export async function getPrescription(): Promise<Prescription> {
  return json<Prescription>(await authed('/me/prescription'))
}

/** 오늘의 처방에서 밀어낸다. 바뀐 처방이 돌아온다. */
export async function skipPrescribed(problemId: string): Promise<Prescription> {
  return json<Prescription>(await authed(`/me/prescription/${problemId}/skip`, { method: 'POST' }))
}

export async function getWeeklyReport(): Promise<WeeklyReport> {
  return json<WeeklyReport>(await authed('/me/report/weekly'))
}

export async function getStats(): Promise<Stats> {
  return json<Stats>(await authed('/me/stats'))
}

/** 문제집 (FR-205). */
export async function getCollections(): Promise<Collection[]> {
  return json<Collection[]>(await authed('/me/collections'))
}

export async function createCollection(name: string): Promise<Collection> {
  const response = await authed('/me/collections', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return json<Collection>(response)
}

export async function addToCollection(collectionId: string, problemId: string): Promise<void> {
  await authed(`/me/collections/${collectionId}/problems/${problemId}`, { method: 'POST' })
}

export async function removeFromCollection(collectionId: string, problemId: string): Promise<void> {
  await authed(`/me/collections/${collectionId}/problems/${problemId}`, { method: 'DELETE' })
}

/** 해설 (FR-214). 잠겨 있으면 본문이 null 이다. */
export async function getEditorial(problemId: string): Promise<Editorial> {
  return json<Editorial>(await authed(`/labs/${problemId}/editorial`))
}

/** 정답 전에 연다. 되돌릴 수 없고, 그 뒤 제출의 증거가 가벼워진다. */
export async function unlockEditorial(problemId: string): Promise<Editorial> {
  return json<Editorial>(await authed(`/labs/${problemId}/editorial/unlock`, { method: 'POST' }))
}

/** 한 입력에 여러 풀이를 돌린다 (§6.4~6.6). */
export async function startLab(problemId: string, args: unknown[], labels: string[]): Promise<LabRun> {
  const response = await authed(`/labs/${problemId}/runs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ args, labels }),
  })
  return json<LabRun>(response)
}

export async function getLab(id: string): Promise<LabRun> {
  return json<LabRun>(await authed(`/labs/runs/${id}`))
}

/** 반례 아레나 (§8.3). */
export async function getArena(problemId: string): Promise<ArenaBoard> {
  return json<ArenaBoard>(await authed(`/arena/${problemId}`))
}

export async function attemptArena(problemId: string, args: unknown[]): Promise<ArenaAttempt> {
  const response = await authed(`/arena/${problemId}/attempts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ args }),
  })
  return json<ArenaAttempt>(response)
}

export async function getArenaAttempt(id: string): Promise<ArenaAttempt> {
  return json<ArenaAttempt>(await authed(`/arena/attempts/${id}`))
}

/** 내 오답을 아레나에 내놓는다 (§8.3). 검수를 거쳐 세워진다. 409 는 이미 내놓은 제출이다. */
export async function donateToArena(problemId: string, submissionId: string, note: string): Promise<ArenaDonation> {
  const response = await authed(`/arena/${problemId}/donations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ submissionId, note }),
  })
  return json<ArenaDonation>(response)
}

export async function getMyDonations(problemId: string): Promise<ArenaDonation[]> {
  return json<ArenaDonation[]>(await authed(`/arena/${problemId}/donations/mine`))
}

/** 세워진 남의 오답을 신고한다 (§8.5). 204 는 이미 신고한 것이다. */
export async function reportArenaTarget(problemId: string, name: string, reason: string): Promise<void> {
  const response = await authed(`/arena/${problemId}/targets/${encodeURIComponent(name)}/reports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!response.ok && response.status !== 204) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
}

/**
 * 최초 분기 진단 (PRD FR-805).
 *
 * 아직 계산되지 않았으면 204 이고 null 이다. 트레이스와 같은 이유로 오류가 아니다 —
 * 참조 실행이 아직 안 돌았거나, 이 문제에 참조 풀이가 없을 수 있다.
 */
export async function getDivergence(submissionId: string): Promise<Divergence | null> {
  const response = await authed(`/submissions/${submissionId}/divergence`)
  if (response.status === 204) return null
  return json<Divergence>(response)
}

/**
 * 전이 확인 과제를 받는다 (PRD FR-807).
 *
 * 도움을 하나도 받지 않았으면 409 다 — 스스로 푼 것은 이미 그 자체로 증거이고, 그 위에
 * 과제를 얹으면 안 받아도 될 숙제가 된다.
 */
export async function assignTransfer(sessionId: string): Promise<TransferTask> {
  const response = await authed(`/coaching/sessions/${sessionId}/transfer`, { method: 'POST' })
  return json<TransferTask>(response)
}

/** 설명 과제를 낸다. 이것을 내야 뒤따르는 판정이 전이 확인이 된다. */
export async function explainTransfer(taskId: string, text: string): Promise<TransferTask> {
  const response = await authed(`/coaching/transfers/${taskId}/explain`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
  return json<TransferTask>(response)
}

/**
 * 내 테스트를 대표 오답에 겨눈다 (PRD FR-804).
 *
 * 기대 출력을 적은 케이스만 보낸다. 입력만 넣고 돌려 본 것은 시험이 아니라 실행이고,
 * 그것으로는 아무 결함도 잡을 수 없다.
 */
export async function startMutationCheck(
  problemId: string,
  cases: TrialCaseInput[],
): Promise<MutationCheck> {
  const response = await authed('/mutations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId, cases }),
  })
  return json<MutationCheck>(response)
}

/**
 * 결과를 가져온다.
 *
 * 시험 실행보다 오래 걸린다 — 정답 한 번에 오답 N 번을 돌리므로 몇 초가 아니라
 * 십수 초다.
 */
export async function getMutationCheck(id: string): Promise<MutationCheck> {
  return json<MutationCheck>(await authed(`/mutations/${id}`))
}

/**
 * 제출한 코드 (PRD §6.4).
 *
 * 목록과 함께 오지 않고 따로 받는다 — 펼쳐 볼 때만 오간다. 계정을 지운 사용자의 소스는
 * 비어 있어 204 가 오고, 그때는 null 이다 (§11.3).
 */
export async function getSubmissionSource(id: string): Promise<string | null> {
  const response = await authed(`/submissions/${id}/source`)
  if (response.status === 204) return null
  const body = await json<{ source: string }>(response)
  return body.source
}

/** 풀이 전 질문과 이미 답한 것 (PRD FR-803). */
export async function getPreQuestions(problemId: string): Promise<PreQuestionSet> {
  return json<PreQuestionSet>(await authed(`/prequestions/${problemId}`))
}

/**
 * 답한다.
 *
 * 채점 결과는 **답한 뒤에** 온다. 미리 알면 예측이 아니라 받아쓰기가 된다.
 */
export async function answerPreQuestion(
  problemId: string,
  kind: QuestionKind,
  answer: string,
  rationale: string | null,
): Promise<AnsweredQuestion> {
  const response = await authed(`/prequestions/${problemId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kind, answer, rationale }),
  })
  return json<AnsweredQuestion>(response)
}

/** 역량 지도 (PRD FR-801, FR-806). */
export async function getCompetencyMap(): Promise<CompetencyMap> {
  return json<CompetencyMap>(await authed('/me/competencies'))
}

/** 한 역량의 근거. 목록에서 여기로 내려온다. */
export async function getEvidence(competency: string): Promise<EvidenceView[]> {
  return json<EvidenceView[]>(await authed(`/me/competencies/${competency}`))
}

/** 표시 이름 변경 (기획서 부록 A 계정). */
export async function rename(displayName: string): Promise<void> {
  const response = await authed('/auth/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ displayName }),
  })
  await json<unknown>(response)

  // 화면 곳곳이 세션의 이름을 읽는다. 서버만 바꾸고 두면 다시 로그인하기 전까지 옛 이름이
  // 남는다.
  const session = getSession()
  if (session) setSession({ ...session, displayName: displayName.trim() })
}

/**
 * 비밀번호 변경.
 *
 * 서버가 열린 세션을 전부 끊고 새 세션을 준다. 받은 것으로 갈아 끼우지 않으면 **방금
 * 비밀번호를 바꾼 사람이 곧바로 튕겨 나간다.**
 */
export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<{ revokedSessions: number }> {
  const response = await authed('/auth/me/password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword }),
  })
  const body = await json<{ session: Session; revokedSessions: number }>(response)
  setSession(body.session)
  return { revokedSessions: body.revokedSessions }
}

/** 내 데이터 전부 (§11.3 반출). */
export async function exportAccount(): Promise<unknown> {
  return json<unknown>(await authed('/auth/me/export'))
}

/** 계정 삭제 (§11.3). 되돌릴 수 없어 비밀번호를 다시 받는다. */
export async function deleteAccount(password: string): Promise<void> {
  const response = await authed('/auth/me', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
  await json<unknown>(response)
  setSession(null)
}

/**
 * 문제 목록 (PRD FR-201~203).
 *
 * 로그인하지 않아도 열린다. 다만 토큰이 있으면 실어 보낸다 — 그래야 서버가 "내가 푼
 * 문제"를 표시해 준다. [authed] 를 쓰지 않는 이유는 그것이 세션이 없을 때 던지기
 * 때문이다. 여기서는 세션이 없는 것이 오류가 아니라 **평범한 경우**다.
 */
export function listProblems(filter: ProblemFilter): Promise<ProblemPage> {
  const params = new URLSearchParams()
  if (filter.query.trim()) params.set('query', filter.query.trim())
  for (const value of filter.difficulty) params.append('difficulty', value)
  for (const value of filter.tags) params.append('tags', value)
  if (filter.status) params.set('status', filter.status)

  const session = getSession()
  const suffix = params.toString() ? `?${params}` : ''
  return fetch(`${BASE}/problems${suffix}`, {
    headers: session ? { Authorization: `Bearer ${session.accessToken}` } : {},
  }).then(json<ProblemPage>)
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

// --- 문제별 질문 게시판 (§8.5) ---

export async function listQuestions(problemId: string): Promise<DiscussionPost[]> {
  return json<DiscussionPost[]>(await authed(`/discussions/${problemId}`))
}

export async function getThread(questionId: string): Promise<DiscussionThread> {
  return json<DiscussionThread>(await authed(`/discussions/threads/${questionId}`))
}

export async function askQuestion(
  problemId: string,
  title: string,
  body: string,
  anchor: DiscussionAnchorRequest | null,
  spoiler: boolean,
): Promise<DiscussionPost> {
  const response = await authed(`/discussions/${problemId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, body, anchor, spoiler }),
  })
  return json<DiscussionPost>(response)
}

export async function answerQuestion(
  questionId: string,
  body: string,
  anchor: DiscussionAnchorRequest | null,
  spoiler: boolean,
): Promise<DiscussionPost> {
  const response = await authed(`/discussions/threads/${questionId}/answers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ body, anchor, spoiler }),
  })
  return json<DiscussionPost>(response)
}

export async function reportPost(postId: string, reason: string): Promise<void> {
  const response = await authed(`/discussions/posts/${postId}/reports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!response.ok && response.status !== 204) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
}

// --- 풀이 공유와 기여 (§8.5) ---

export async function listSolutions(problemId: string): Promise<DiscussionPost[]> {
  return json<DiscussionPost[]>(await authed(`/discussions/${problemId}/solutions`))
}

export async function shareSolution(problemId: string, submissionId: string, title: string, body: string): Promise<DiscussionPost> {
  const response = await authed(`/discussions/${problemId}/solutions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ submissionId, title, body }),
  })
  return json<DiscussionPost>(response)
}

export async function markHelpful(postId: string): Promise<void> {
  const response = await authed(`/discussions/posts/${postId}/helpful`, { method: 'POST' })
  if (!response.ok && response.status !== 204) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
}

export async function getContributions(): Promise<Contributions> {
  return json<Contributions>(await authed('/discussions/me/contributions'))
}

// --- 제재와 이의 (§8.5, §10.4) ---

export async function getMySanction(): Promise<SanctionView | null> {
  const me = await json<{ sanction: SanctionView | null }>(await authed('/auth/me'))
  return me.sanction
}

export async function appealSanction(id: string, text: string): Promise<SanctionView> {
  const response = await authed(`/auth/me/sanction/${id}/appeal`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
  return json<SanctionView>(response)
}

// --- 대회와 미니 대결 (§8.4) ---

export async function listContests(): Promise<ContestSummary[]> {
  return json<ContestSummary[]>(await authed('/contests'))
}

export async function getContest(id: string): Promise<ContestView> {
  return json<ContestView>(await authed(`/contests/${id}`))
}

export async function joinContest(id: string): Promise<void> {
  const response = await authed(`/contests/${id}/join`, { method: 'POST' })
  if (!response.ok && response.status !== 204) {
    throw new ApiFailure(response.status, (await response.json()) as ApiError)
  }
}

export async function openDuel(problemId: string, minutes: number): Promise<{ contest: ContestSummary; joinCode: string }> {
  const response = await authed('/contests/duels', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId, minutes }),
  })
  return json<{ contest: ContestSummary; joinCode: string }>(response)
}

export async function joinDuel(code: string): Promise<{ contest: ContestSummary }> {
  const response = await authed('/contests/duels/join', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  })
  return json<{ contest: ContestSummary }>(response)
}

export async function startVirtual(contestId: string): Promise<{ contest: ContestSummary }> {
  const response = await authed(`/contests/${contestId}/virtual`, { method: 'POST' })
  return json<{ contest: ContestSummary }>(response)
}

export async function getMyRating(): Promise<Rating> {
  return json<Rating>(await authed('/contests/me/rating'))
}
