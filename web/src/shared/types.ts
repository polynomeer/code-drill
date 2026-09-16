/**
 * Control Plane 과 공유하는 값 타입.
 *
 * 백엔드의 `SubmissionStatus`(§4.2), `Verdict`(§14.1), `TraceEvent`(§7.2)와 짝을 이룬다.
 * 한쪽만 바꾸면 계약이 깨지므로 함께 고친다.
 */

export type SubmissionStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'LEASED'
  | 'COMPILING'
  | 'RUNNING'
  | 'AGGREGATING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'SYSTEM_ERROR'

export type Verdict =
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILE_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIME_LIMIT'
  | 'MEMORY_LIMIT'
  | 'OUTPUT_LIMIT'
  | 'SYSTEM_ERROR'

/** 진행 중 상태. 이 동안 UI 는 판정 결과를 단정하지 않는다 (§0.2 No false precision). */
export const IN_FLIGHT: ReadonlySet<SubmissionStatus> = new Set([
  'CREATED',
  'QUEUED',
  'LEASED',
  'COMPILING',
  'RUNNING',
  'AGGREGATING',
])

export const VERDICT_LABEL: Record<Verdict, string> = {
  ACCEPTED: '정답',
  WRONG_ANSWER: '오답',
  COMPILE_ERROR: '컴파일 실패',
  RUNTIME_ERROR: '런타임 오류',
  TIME_LIMIT: '시간 초과',
  MEMORY_LIMIT: '메모리 초과',
  OUTPUT_LIMIT: '출력 초과',
  SYSTEM_ERROR: '시스템 오류',
}

export interface Problem {
  id: string
  version: number
  title: string
  statement: string
  timeMillis: number
  memoryMb: number
  signature: string
  /** 공개 샘플만 담긴다. 숨은 케이스는 서버가 애초에 내려보내지 않는다 (§9.1). */
  samples: { id: string; args: unknown[]; expected: unknown }[]
  groups: GroupInfo[]
}

/** 그룹별 배점. `SUM` 은 통과 비율만큼 부분 점수를 준다 (§6.2). */
export interface GroupInfo {
  id: string
  weight: number
  aggregation: 'ALL_OR_NOTHING' | 'SUM'
  caseCount: number
}

/** 난이도 (기획서 §10.2). 값과 순서는 백엔드 `Difficulty` 와 짝을 이룬다. */
export const DIFFICULTIES = ['INTRO', 'EASY', 'MEDIUM', 'HARD', 'EXPERT'] as const
export type Difficulty = (typeof DIFFICULTIES)[number]

export const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  INTRO: '입문',
  EASY: '쉬움',
  MEDIUM: '보통',
  HARD: '어려움',
  EXPERT: '최상',
}

export interface ProblemSummary {
  id: string
  version: number
  title: string
  difficulty: Difficulty
  tags: string[]
  competencies: string[]
  /** 표본이 적으면 null 이다. 없는 값을 0% 로 그리지 않는다 (§0.2 No false precision). */
  solvedRate: number | null
  solved: boolean
}

/**
 * 문제 목록 응답 (PRD FR-201~203).
 *
 * 백엔드 `ProblemPage` 와 짝을 이룬다. `total` 은 **조건에 맞는 전체 개수**이지 이번
 * 페이지의 개수가 아니다.
 */
export interface ProblemPage {
  items: ProblemSummary[]
  nextCursor: string | null
  total: number
  /** 태그 → 이 조건에서의 문제 수. 결과가 있는 태그만 온다. */
  tags: Record<string, number>
}

/** 목록 필터 (PRD FR-201). 비어 있는 항목은 요청에 싣지 않는다. */
export interface ProblemFilter {
  query: string
  difficulty: Difficulty[]
  tags: string[]
  status: 'SOLVED' | 'UNSOLVED' | null
}

export const EMPTY_FILTER: ProblemFilter = { query: '', difficulty: [], tags: [], status: null }

export type SubmissionLanguage = 'KOTLIN' | 'JAVA' | 'PYTHON'

export const LANGUAGE_LABEL: Record<SubmissionLanguage, string> = {
  KOTLIN: 'Kotlin',
  JAVA: 'Java',
  PYTHON: 'Python',
}

/** Monaco 언어 id. 채점 언어와 이름이 같지는 않다. */
export const EDITOR_LANGUAGE: Record<SubmissionLanguage, string> = {
  KOTLIN: 'kotlin',
  JAVA: 'java',
  PYTHON: 'python',
}

export interface CaseResult {
  caseId: string
  groupId: string
  verdict: Verdict
  measurements: { wallTimeMillis: number; peakMemoryBytes: number }
  message: string | null
}

export interface GroupResult {
  groupId: string
  verdict: Verdict
  score: number
  maxScore: number
  cases: CaseResult[]
}

export interface Submission {
  id: string
  problemId: string
  problemVersion: number
  language: string
  status: SubmissionStatus
  verdict: Verdict | null
  score: number | null
  compileLog: string | null
  groups: GroupResult[] | null
  /** 내 제출인가. 게시판에 붙어 공유된 남의 제출을 열면 false 다 (§8.5). 목록에서는 오지 않는다 — 전부 내 것이다. */
  mine?: boolean
}

/* 트레이스 타입은 features/replay/traceTypes.ts 에 있다. 렌더러와 함께 두어야
   스키마를 바꿀 때 그리는 쪽과 계약이 같이 움직인다. */

/** cursor 페이지네이션 응답 (§9.1). nextCursor 가 null 이면 마지막 페이지다. */
export interface Page<T> {
  items: T[]
  nextCursor: string | null
}

/** 작업 중인 초안. version 은 CAS 용이며 저장할 때마다 오른다 (§8.2). */
export interface Draft {
  problemId: string
  language: string
  code: string
  version: number
  updatedAt: string
}

export interface DraftConflict {
  error: ApiError
  current: Draft
}

/** 표준 오류 응답 (§9.1). traceId 는 문의·조사에 그대로 쓴다. */
export interface ApiError {
  errorCode: string
  message: string
  traceId: string
}

/** 시험 실행 (기획서 부록 A 실행 도메인). 백엔드 `TrialRun` 과 짝을 이룬다. */
export type TrialStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface TrialCaseResult {
  index: number
  outcome: Verdict
  /** 사용자 코드가 실제로 내놓은 값. 실행되지 못했으면 null. */
  actual: string | null
  message: string | null
  wallTimeMillis: number
  peakMemoryBytes: number
}

export interface Trial {
  id: string
  problemId: string
  language: SubmissionLanguage
  status: TrialStatus
  compileLog: string | null
  cases: TrialCaseResult[]
}

/** 사용자가 적는 한 건. `expected` 는 없어도 된다. */
export interface TrialCaseInput {
  args: unknown[]
  expected?: unknown
}

/**
 * 코칭 세션 (PRD FR-802).
 *
 * 사다리 본문은 **펼친 것만** 온다. 남은 단계는 개수만 안다 — 통째로 받아 놓고 화면에서
 * 가리면 개발자 도구를 열 줄 아는 사람에게는 도움이 아니라 정답이다.
 */
export interface CoachingSession {
  id: string
  problemId: string
  /** 비어 있을 수 있다. 약한 역량이 없으면 도울 것도 없다. */
  focus: CoachingFocus[]
  revealed: RevealedHint[]
  /** 가장 깊이 본 단계. 0 이면 도움 없이 풀고 있다는 뜻이다. */
  helpLevel: number
  closed: boolean
}

export interface CoachingFocus {
  competency: string
  remaining: number
}

export interface RevealedHint {
  competency: string
  level: number
  text: string
}

/**
 * 최초 분기 진단 (PRD FR-805).
 *
 * 참조 코드도 참조 트레이스도 오지 않는다. 오는 것은 갈라진 그 한 이벤트를 사람 말로
 * 옮긴 한 줄뿐이고, 접근 자체가 다르면 그것마저 오지 않는다.
 */
export type DivergenceOutcome =
  | 'PENDING'
  | 'SAME'
  | 'DIVERGED'
  | 'DIFFERENT_APPROACH'
  | 'NO_REFERENCE'

export interface Divergence {
  submissionId: string
  caseId: string
  outcome: DivergenceOutcome
  sharedPrefix: number | null
  /** 갈라진 지점의 이벤트 seq. 리플레이가 이 자리로 이동한다. */
  divergedAtSeq: number | null
  /** 그 이벤트를 부른 내 코드의 줄. */
  sourceLine: number | null
  expectedStep: string | null
  actualStep: string | null
}

/**
 * 리플레이 중 다음 상태 예측 (PRD FR-805).
 *
 * 채점은 서버가 한다. 화면이 맞고 틀림을 정하면 그것은 채점이 아니라 자기 신고다.
 */
export interface StatePrediction {
  id: string
  submissionId: string
  /** 맞히려 한 이벤트의 seq. */
  step: number
  predicted: string
  actual: string
  correct: boolean
  rationale: string | null
}

/**
 * 최소 반례 (기술 설계서 §6.3).
 *
 * 줄인 입력 하나와 그 입력의 정답만 온다. 숨은 테스트 묶음은 오지 않는다.
 */
export type CounterexampleStatus = 'PENDING' | 'FOUND' | 'NOT_REPRODUCED' | 'FAILED'

export interface Counterexample {
  submissionId: string
  status: CounterexampleStatus
  message: string | null
  args: unknown[] | null
  actual: string | null
  expected: string | null
  originalSize: number | null
  minimalSize: number | null
  rounds: number | null
}

/**
 * 학습 루프 (PRD FR-808, FR-205).
 */
export type Reason =
  | 'RECENT_FAILURE'
  | 'TRANSFER'
  | 'HINT_DEPENDENT'
  | 'REVIEW_DUE'
  | 'WEAK_COMPETENCY'
  | 'NEXT_ON_PATH'

export const REASON_LABEL: Record<Reason, string> = {
  RECENT_FAILURE: '최근에 틀린 문제',
  TRANSFER: '전이 확인',
  HINT_DEPENDENT: '힌트 없이 다시',
  REVIEW_DUE: '복습 시점',
  WEAK_COMPETENCY: '약점 보완',
  NEXT_ON_PATH: '다음 단계',
}

export interface PrescribedProblem {
  problemId: string
  reason: Reason
  detail: string
  competency: string | null
  nextMeasurement: string
}

export interface Streak {
  days: number
  activeToday: boolean
  atRisk: boolean
}

export interface Prescription {
  date: string
  items: PrescribedProblem[]
  streak: Streak
}

export interface WeeklyReport {
  from: string
  to: string
  activity: { attempts: number; accepted: number; problemsSolved: number; activeDays: number }
  weakest: string[]
  recurrences: { problemId: string; solvedAt: string; failedAt: string }[]
  growth: { competency: string; from: MasteryLevel; to: MasteryLevel }[]
  actions: PrescribedProblem[]
  nextMeasurement: string
}

export interface Stats {
  problemsAttempted: number
  problemsSolved: number
  submissions: number
  accepted: number
  verdicts: Record<string, number>
  byTag: Record<string, { attempted: number; solved: number }>
}

export interface Collection {
  id: string
  name: string
  problems: string[]
}

/**
 * 해설과 실험실 (기획서 §6.4~6.6, PRD FR-214).
 */
export interface Editorial {
  problemId: string
  available: boolean
  locked: boolean
  solved: boolean
  body: string | null
  approaches: string[]
}

export type LabStatus = 'PENDING' | 'COMPLETED'

export interface LabRun {
  id: string
  problemId: string
  args: unknown[]
  labels: string[]
  status: LabStatus
  results: ApproachResult[]
}

export interface ApproachResult {
  label: string
  verdict: Verdict
  actual: string | null
  measurements: { cpuTimeMillis: number; wallTimeMillis: number; peakMemoryBytes: number; outputBytes: number }
  eventCounts: Record<string, number>
  events: import('../features/replay/traceTypes').TraceEvent[]
  truncated: boolean
}

/**
 * 반례 아레나 (기획서 §8.3).
 *
 * 오답의 소스가 온다 — 이 문제를 맞힌 사람에게만 나가는 응답이다.
 */
export interface ArenaBoard {
  problemId: string
  locked: boolean
  targets: ArenaTarget[]
  records: ArenaRecord[]
}

export interface ArenaTarget {
  name: string
  kind: DefectKind
  kindLabel: string
  note: string
  source: string
  /** 저작자의 대표 오답이 아니라 검수를 거쳐 세운 남의 오답 (§8.3). 신고는 이쪽에만. */
  community: boolean
}

/** 내가 아레나에 내놓은 오답 (§8.3 익명화된 오답, §8.5 검수). 소스는 실리지 않는다. */
export interface ArenaDonation {
  id: string
  problemId: string
  submissionId: string
  note: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETIRED'
  kind: DefectKind | null
  reason: string | null
  createdAt: string
  targetName: string
}

export interface ArenaRecord {
  mutantName: string
  breakers: number
  smallestSize: number | null
  smallestIsMine: boolean
  firstIsMine: boolean
  myBest: number | null
}

export type ArenaStatus = 'PENDING' | 'COMPLETED' | 'INVALID_INPUT' | 'FAILED'

export interface ArenaAttempt {
  id: string
  problemId: string
  args: unknown[]
  status: ArenaStatus
  message: string | null
  results: {
    name: string
    kind: DefectKind
    broken: boolean
    actual: string | null
    minimalArgs: unknown[] | null
    minimalSize: number | null
  }[]
}

/**
 * 전이 확인 과제 (PRD FR-807).
 *
 * 왜 이 문제가 골라졌는지는 오지 않는다 — "같은 역량을 요구한다"는 말이 곧 "같은 생각으로
 * 풀린다"는 힌트가 되기 때문이다.
 */
export type TransferStatus = 'ASSIGNED' | 'EXPLAINED' | 'VERIFIED' | 'UNVERIFIED'

export interface TransferTask {
  id: string
  sourceProblemId: string
  targetProblemId: string
  explanation: string | null
  status: TransferStatus
}

/**
 * 변이 평가 (PRD FR-804).
 *
 * 백엔드 `MutationResponse` 와 짝을 이룬다. **오답의 이름도 소스도 오지 않는다** —
 * 오는 것은 결함군과 그것을 잡은 내 케이스 번호까지다.
 */
export type MutationStatus = 'PENDING' | 'COMPLETED' | 'NO_CASES' | 'FAILED'

export type DefectKind =
  | 'OFF_BY_ONE'
  | 'MISSING_EDGE_CASE'
  | 'WRONG_BRANCH'
  | 'WRONG_ALGORITHM'
  | 'PERFORMANCE'
  | 'UNSPECIFIED'

export interface MutationCheck {
  id: string
  problemId: string
  status: MutationStatus
  message: string | null
  /** 기대 출력이 실제 정답과 다른 케이스 번호 (1부터). 정답 값은 오지 않는다. */
  mistakenCases: number[]
  /** 손으로 잡을 수 있는 결함군만 센 비율. 잴 것이 없으면 null 이다. */
  score: number | null
  kinds: KindSummary[]
}

export interface KindSummary {
  kind: DefectKind
  label: string
  killed: number
  total: number
  /** 점수에 들어가는지. 성능 결함은 손으로 적는 케이스로 잡을 수 없어 빠진다. */
  scored: boolean
  killedBy: number[]
}

/** 풀이 전 질문 (PRD FR-803). 백엔드 `PreQuestion` 과 짝을 이룬다. */
export type QuestionKind = 'ALGORITHM_CHOICE' | 'TIME_COMPLEXITY' | 'SPACE_COMPLEXITY'
export type Misconception = 'UNDER_ESTIMATED' | 'OVER_ESTIMATED' | 'WRONG_TECHNIQUE'

export const MISCONCEPTION_LABEL: Record<Misconception, string> = {
  UNDER_ESTIMATED: '실제보다 싸게 봤습니다',
  OVER_ESTIMATED: '실제보다 비싸게 봤습니다',
  WRONG_TECHNIQUE: '이 문제가 쓰는 기법이 아닙니다',
}

export interface PreQuestion {
  kind: QuestionKind
  prompt: string
  choices: string[]
}

export interface AnsweredQuestion {
  kind: QuestionKind
  answer: string
  correct: boolean
  misconception: Misconception | null
  /** 틀렸을 때만 온다. */
  expected: string | null
}

export interface PreQuestionSet {
  questions: PreQuestion[]
  answered: AnsweredQuestion[]
}

/** 역량 지도 (PRD §3.4, FR-801·FR-806). 백엔드 `MasteryView` 와 짝을 이룬다. */
export type MasteryLevel = 'UNMEASURED' | 'DEVELOPING' | 'PROFICIENT' | 'STRONG'
export type Confidence = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH'
export type EvidenceSource =
  | 'SUBMISSION'
  | 'PREQUESTION'
  | 'TRIAL'
  | 'MUTATION'
  | 'TRANSFER'
  | 'PREDICTION'

export const LEVEL_LABEL: Record<MasteryLevel, string> = {
  UNMEASURED: '아직 재지 않음',
  DEVELOPING: '기르는 중',
  PROFICIENT: '해낸다',
  STRONG: '단단하다',
}

export const CONFIDENCE_LABEL: Record<Confidence, string> = {
  NONE: '근거 없음',
  LOW: '근거 적음',
  MEDIUM: '근거 보통',
  HIGH: '근거 충분',
}

export const GROUP_LABEL: Record<string, string> = {
  UNDERSTANDING: '이해',
  DESIGN: '설계',
  EXECUTION: '실행',
  VERIFICATION: '검증',
  EXTENSION: '확장',
}

export const COMPETENCY_LABEL: Record<string, string> = {
  READING: '문제 독해',
  CONSTRAINTS: '조건 추출',
  MODELING: '모델링',
  ALGORITHM_CHOICE: '알고리즘 선택',
  CORRECTNESS: '논리·정확성',
  IMPLEMENTATION: '구현력',
  COMPLEXITY: '복잡도 예측',
  OPTIMIZATION: '최적화',
  TEST_DESIGN: '테스트 설계',
  EDGE_CASES: '엣지케이스',
  COUNTEREXAMPLE: '반례',
  DEBUGGING: '디버깅',
  EXPLANATION: '설명',
  TRANSFER: '전이',
  METACOGNITION: '메타인지',
  AI_COLLABORATION: 'AI 협업',
}

export const SOURCE_LABEL: Record<EvidenceSource, string> = {
  SUBMISSION: '제출',
  PREQUESTION: '풀기 전 질문',
  TRIAL: '내가 만든 테스트',
  MUTATION: '테스트 점검',
  TRANSFER: '전이 확인',
  PREDICTION: '상태 예측',
}

export interface MasteryView {
  competency: string
  group: string
  level: MasteryLevel
  confidence: Confidence
  evidenceCount: number
  successCount: number
}

export interface CompetencyMap {
  /** 증거가 하나라도 있는가. false 면 진단 미완료다 (FR-801). */
  diagnosed: boolean
  competencies: MasteryView[]
}

export interface EvidenceView {
  source: EvidenceSource
  success: boolean
  /** 도움 수준. 1.0 이면 도움 없이 얻은 증거다. */
  weight: number
  problemId: string
  reference: string | null
  detail: string | null
  occurredAt: string
}

/**
 * 문제별 질문 게시판 (기획서 §8.5 "질문 게시판과 코드 구간 링크", "리플레이 시점을 공유하는 주석").
 *
 * 글쓴이의 id 는 오지 않는다 — 내 것인가만 온다. 풀이를 드러내는 글은 맞힌 사람이 아니면
 * 본문과 붙인 자리가 비고 `locked` 가 선다.
 */
export type PostKind = 'QUESTION' | 'ANSWER' | 'SOLUTION'
export type ContributorTier = 'NEW' | 'ACTIVE' | 'TRUSTED'

export interface DiscussionPost {
  id: string
  problemId: string
  kind: PostKind
  parentId: string | null
  title: string | null
  body: string
  anchor: DiscussionAnchor | null
  spoiler: boolean
  locked: boolean
  mine: boolean
  erased: boolean
  answerCount: number
  /** 남이 남긴 도움됐다의 수와 내가 남겼는지 (§8.5 평판). */
  helpful: number
  markedHelpful: boolean
  /** 글쓴이의 등급 — 이름 대신 나가는 유일한 것. 지운 계정이면 null. */
  contributor: ContributorTier | null
  createdAt: string
}

/** 내 기여 (§8.5 기여자 평판). 수치는 본인에게만 보인다. */
export interface Contributions {
  helpfulReceived: number
  solutionsShared: number
  answers: number
  donationsApproved: number
  score: number
  tier: ContributorTier
}

/** 글에 붙인 자기 제출의 한 자리 — 코드 구간(줄 범위와 그 줄들)이거나 리플레이의 걸음이거나 둘 다. */
export interface DiscussionAnchor {
  submissionId: string
  lineFrom: number | null
  lineTo: number | null
  step: number | null
  excerpt: string | null
}

export interface DiscussionThread {
  question: DiscussionPost
  answers: DiscussionPost[]
}

export interface DiscussionAnchorRequest {
  submissionId: string
  lineFrom?: number
  lineTo?: number
  step?: number
}

/** 내 계정의 제재 (§8.5 단계적 제재, §10.4 이의 절차). 풀린 것도 온다 — 이의의 답이 여기 실린다. */
export interface SanctionView {
  id: string
  kind: 'WARNING' | 'MUTE' | 'SUSPEND'
  reason: string
  startsAt: string
  endsAt: string | null
  active: boolean
  liftedAt: string | null
  appealed: boolean
  appealResolution: 'UPHELD' | 'LIFTED' | null
  appealNote: string | null
}

/** 대회와 미니 대결 (§8.4). 상태는 시각에서 읽는다. */
export type ContestStatus = 'DRAFT' | 'WAITING' | 'SCHEDULED' | 'RUNNING' | 'FINISHED'

export interface ContestSummary {
  id: string
  kind: 'CONTEST' | 'DUEL' | 'HACK' | 'VIRTUAL'
  title: string
  status: ContestStatus
  startsAt: string | null
  endsAt: string | null
  minutes: number | null
  joined: boolean
  entrants: number
  problemCount: number
  /** 레이팅 대회인가, 끝난 뒤 적용됐는가 (§8.4). */
  rated: boolean
  ratedAt: string | null
}

export interface Standing {
  rank: number
  displayName: string
  mine: boolean
  /** 가상 참가로 낸 줄 (§8.4). 원래 순위표에는 없다. */
  virtual: boolean
  total: number
  solved: number
  lastSolvedAt: string | null
  elapsedSeconds: number | null
  perProblem: Record<string, number>
  /** 레이팅이 적용됐으면 변화. */
  ratingChange: number | null
}

/** 내 레이팅 (§8.4). 변화의 합이라 언제든 다시 셀 수 있다. */
export interface Rating {
  rating: number
  contests: number
  history: { contestId: string; title: string; rank: number; before: number; after: number; appliedAt: string }[]
}

export interface ContestView {
  contest: ContestSummary
  problems: string[]
  standings: Standing[]
  /** 대결의 코드. 만든 사람에게만 온다. */
  joinCode: string | null
  /** 끝난 대회에서 내가 돌고 있는 가상 참가의 id. */
  virtual: string | null
}

// --- 프로젝트형 문제 (feature-roadmap 11단계) ---

export interface ProjectSummary {
  id: string
  version: number
  title: string
  language: string
  difficulty: Difficulty
  tags: string[]
  summary: string
  solved: boolean
}

/** 상세. `files` 가 시작 저장소다 — 편집기가 여는 것. 숨은 테스트는 어디에도 없다. */
export interface ProjectView {
  id: string
  version: number
  title: string
  language: string
  difficulty: Difficulty
  tags: string[]
  statement: string
  files: Record<string, string>
  limits: { buildSeconds: number; testSeconds: number; memoryMb: number; maxFiles: number; maxTotalBytes: number }
  publicTests: string[]
}

export interface ProjectTestOutcome {
  module: string
  name: string
  passed: boolean
  message: string | null
}

/** 프로젝트형 제출. 공개 테스트는 이름과 사유로, 숨은 테스트는 두 수로만 온다. */
export interface ProjectSubmission {
  id: string
  projectId: string
  projectVersion: number
  language: string
  status: 'QUEUED' | 'LEASED' | 'COMPLETED'
  verdict: Verdict | null
  score: number | null
  log: string | null
  tests: ProjectTestOutcome[]
  hiddenPassed: number | null
  hiddenTotal: number | null
  createdAt: string
  completedAt: string | null
  files: Record<string, string> | null
}
