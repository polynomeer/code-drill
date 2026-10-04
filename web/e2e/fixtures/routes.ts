import type { Page } from '@playwright/test'

/**
 * 주요 라우트를 그리는 데 드는 API 응답 한 벌 — 다크 검수(dark.e2e.ts)와 시각 회귀(visual.visual.ts)가 함께 쓴다.
 * 모양은 실제 API 그대로다. 바뀌면 shared/types.ts 와 함께 고친다.
 */
/** 고정 시각 — 시각 회귀가 "3분 전" 같은 글자로 흔들리지 않게. 다크 검수도 같은 값을 쓴다 */
export const NOW = '2026-10-05T03:00:00.000Z'
const now = NOW
const PROBLEM = {
  id: 'two-sum',
  number: 1000,
  version: 1,
  title: '두 수의 합',
  statement: '# 두 수의 합\n\n정수 배열에서 합이 `target` 인 두 원소를 찾는다.\n\n## 제약\n\n- `2 <= n`',
  timeMillis: 2000,
  memoryMb: 256,
  signature: 'fun twoSum(nums: IntArray, target: Int): IntArray',
  samples: [{ id: '01', args: [[2, 7, 11, 15], 9], expected: [0, 1] }],
  groups: [{ id: 'sample', weight: 0, aggregation: 'ALL_OR_NOTHING', caseCount: 1 }],
}
const SUMMARY = { ...PROBLEM, difficulty: 'EASY', tags: ['hash'], competencies: [], solvedRate: 0.5, solvedCount: 12, solved: true }
const SUBMISSION = { id: 's1', problemId: 'two-sum', problemVersion: 1, language: 'PYTHON', status: 'COMPLETED', verdict: 'WRONG_ANSWER', score: 0, compileLog: null, revision: 1, createdAt: now, mine: true,
  groups: [{ groupId: 'sample', verdict: 'WRONG_ANSWER', score: 0, maxScore: 0, cases: [{ caseId: '01', groupId: 'sample', verdict: 'WRONG_ANSWER', measurements: { wallTimeMillis: 3, peakMemoryBytes: 1000 }, message: null, actual: '0,0' }] }] }
const EVENTS = [1, 2, 3].map((seq) => ({ seq, logicalTime: seq, eventType: seq === 3 ? 'MATCH' : 'COMPARE', targetKind: 'ARRAY', targetRef: String(seq - 1), before: null, after: null, importance: seq === 3 ? 3 : 2, sourceLine: seq + 1, attributes: {} }))
const CONTEST = { id: 'c1', kind: 'CONTEST', title: '주간 대회', status: 'RUNNING', startsAt: now, endsAt: '2026-10-05T04:00:00.000Z', minutes: 60, joined: true, entrants: 2, problemCount: 1, rated: true, ratedAt: null }

const BODIES: Record<string, unknown> = {
  '/problems': { items: [SUMMARY], nextCursor: null, total: 1, tags: { hash: 1 }, page: 1, pageCount: 1 },
  '/problems/two-sum': PROBLEM,
  '/me/prescription': { date: '', items: [{ problemId: 'two-sum', reason: 'DIAGNOSTIC', detail: '진단 — 이해 역량의 첫 근거를 만듭니다.', competency: 'READING', nextMeasurement: now }], streak: { days: 3, activeToday: false, atRisk: true } },
  '/me/onboarding': { dailyGoal: 2, language: 'PYTHON', level: 'BEGINNER' },
  '/coaching/sessions': [{ id: 's', problemId: 'two-sum', focus: [{ competency: 'MODELING', remaining: 1 }], revealed: [], helpLevel: 1, closed: false }],
  '/coaching/transfers': [],
  '/me/competencies': { diagnosed: true, competencies: [
    { competency: 'READING', group: 'UNDERSTANDING', level: 'PROFICIENT', confidence: 'HIGH', evidenceCount: 8, successCount: 7 },
    { competency: 'MODELING', group: 'DESIGN', level: 'DEVELOPING', confidence: 'LOW', evidenceCount: 2, successCount: 1 },
    { competency: 'EDGE_CASES', group: 'VERIFICATION', level: 'UNMEASURED', confidence: 'NONE', evidenceCount: 0, successCount: 0 },
  ] },
  '/me/competencies/MODELING': [{ source: 'SUBMISSION', success: false, weight: 0.5, problemId: 'two-sum', reference: 's1', detail: '숨은 그룹', occurredAt: now }],
  '/contests': [CONTEST],
  '/contests/finished': { items: [{ ...CONTEST, id: 'c0', status: 'FINISHED', title: '지난 대회' }], page: 1, pageCount: 1, total: 1 },
  '/contests/c1': { contest: CONTEST, problems: ['two-sum'], standings: [
    { rank: 1, displayName: '에이다', mine: false, virtual: false, total: 100, solved: 1, lastSolvedAt: null, elapsedSeconds: 600, perProblem: { 'two-sum': 100 }, ratingChange: null },
    { rank: 2, displayName: '시험', mine: true, virtual: false, total: 40, solved: 0, lastSolvedAt: null, elapsedSeconds: null, perProblem: { 'two-sum': 40 }, ratingChange: null },
  ], joinCode: null, virtual: null },
  '/submissions': { items: [SUBMISSION], nextCursor: null },
  '/submissions/s1': SUBMISSION,
  '/submissions/s1/source': { source: 'def two_sum(nums, target):\n    return [0, 0]\n\n\n' },
  '/submissions/s1/trace': { schemaVersion: '2.0', traceId: 't', submissionId: 's1', caseId: '01', status: 'READY', eventCount: 3, chunks: [{ index: 0, firstSeq: 1, lastSeq: 3, eventCount: 3 }], summary: EVENTS, truncated: false, diagnostics: null },
  '/submissions/s1/trace/chunks/0': { schemaVersion: '2.0', traceId: 't', index: 0, events: EVENTS },
  '/submissions/s1/divergence': { submissionId: 's1', caseId: '01', outcome: 'DIVERGED', sharedPrefix: 1, divergedAtSeq: 2, sourceLine: 3, expectedStep: 'COMPARE [2]', actualStep: 'COMPARE array[1]' },
  '/submissions/s1/predictions': [],
  '/profiles/ada': { handle: 'ada', displayName: '에이다', joinedAt: now, mine: false, public: true, solved: { total: 3, byDifficulty: { EASY: 3 } },
    activity: Array.from({ length: 365 }, (_, i) => ({ date: new Date(Date.parse(NOW) - (364 - i) * 86400_000).toISOString().slice(0, 10), submissions: i % 5 })),
    streak: { current: 2, longest: 5 }, rating: { rating: 1512, contests: 1, history: [{ contestId: 'c0', title: '지난 대회', rank: 2, before: 1500, after: 1512, at: now }] }, solutions: [], contributorTier: 'ACTIVE' },
  '/me/notifications': { unread: 1, items: [{ id: 'n', kind: 'ANSWERED', title: '내 질문에 답이 달렸습니다', body: null, link: '/', at: now, unread: true }] },
  '/admin/me': { userId: 'op', roles: ['REVIEWER', 'JUDGE_OPERATOR'] },
  '/admin/rejudges': [{ id: 'r', scope: 'problem:two-sum', reason: '케이스 수정', status: 'REQUESTED', requestedBy: 'x', approvedBy: null, dryRun: true, targetCount: 3, createdAt: now }],
  '/admin/discussions/queue': [],
  '/admin/arena/queue': { pending: [], reports: [] },
  '/admin/integrity/queue': [],
}

export async function mockApi(page: Page, theme: 'light' | 'dark' = 'dark') {
  await page.addInitScript(() => {
    localStorage.setItem('codedrill.session', JSON.stringify({ accessToken: 't', refreshToken: 'r', userId: 'u1', displayName: '시험' }))
  })
  await page.addInitScript((value) => localStorage.setItem('codedrill.theme', value), theme)
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname.replace('/api/v1', '')
    if (path === '/events') return route.fulfill({ status: 202 })
    if (path.startsWith('/workspaces/') || path.endsWith('/events')) return route.fulfill({ status: 204 })
    const body = BODIES[path]
    if (body === undefined) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ errorCode: 'NOT_FOUND', message: '없음', traceId: '' }) })
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  })
}

