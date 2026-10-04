import { authed } from '../../api/client'

/**
 * 운영 API (control-plane/admin AdminController) — 운영 콘솔 청크에만 들어간다.
 *
 * 일반 API 와 오류 모양이 다르다. 거절(2인 원칙·이미 결정됨)은 409 `{ reason }`, 역할 부족은
 * 403 `{ code, message }` 다. 화면이 둘을 같은 문장으로 보이도록 여기서 한 모양으로 바꾼다.
 */
export class AdminError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'AdminError'
  }
}

async function admin<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await authed(`/admin${path}`, {
    ...init,
    headers: init.body ? { 'Content-Type': 'application/json', ...(init.headers ?? {}) } : init.headers,
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { reason?: string; message?: string }
    throw new AdminError(response.status, body.reason ?? body.message ?? `요청이 거절됐습니다 (${response.status})`)
  }
  const text = await response.text()
  return (text ? JSON.parse(text) : null) as T
}

const post = <T>(path: string, body?: unknown) =>
  admin<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

/* ─── 형 ─── */

export type AdminRole = 'CONTENT_EDITOR' | 'REVIEWER' | 'PUBLISHER' | 'JUDGE_OPERATOR' | 'SECURITY_ADMIN'

export const ROLE_LABEL: Record<AdminRole, string> = {
  CONTENT_EDITOR: '콘텐츠 편집',
  REVIEWER: '검수',
  PUBLISHER: '공개',
  JUDGE_OPERATOR: '채점 운영',
  SECURITY_ADMIN: '보안 관리',
}

export interface Me {
  userId: string
  roles: AdminRole[]
}

export interface PendingVersion {
  versionId: string
  problemId: string
  version: number
  packageDigest: string
  reportDigest: string
  validatorVersion: string
  registeredBy: string
  registeredAt: string
  publishedVersion: number | null
}

export type RejudgeStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RUNNING' | 'COMPLETED'

export interface RejudgeJob {
  id: string
  scope: string
  reason: string
  status: RejudgeStatus
  requestedBy: string
  approvedBy: string | null
  dryRun: boolean
  targetCount: number
  createdAt: string
}

export interface RejudgeReport {
  job: RejudgeJob
  completed: number
  changes: { submissionId: string; from: string; to: string }[]
}

export interface DiscussionPost {
  id: string
  problemId: string
  kind: 'QUESTION' | 'ANSWER' | 'SOLUTION'
  authorId: string | null
  title: string | null
  body: string
  status: 'VISIBLE' | 'HIDDEN'
  createdAt: string
}

export interface Report {
  id: string
  reporterId: string
  reason: string
  status: 'OPEN' | 'RETIRED' | 'DISMISSED'
  createdAt: string
}

export interface ReportedPost {
  report: Report & { postId: string }
  post: DiscussionPost | null
}

export interface ArenaDonation {
  id: string
  problemId: string
  submissionId: string
  donorUserId: string
  source: string | null
  note: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETIRED'
  kind: string | null
  createdAt: string
}

export interface ArenaQueue {
  pending: ArenaDonation[]
  reports: { report: Report & { donationId: string }; donation: ArenaDonation | null }[]
}

export interface FlaggedPair {
  flag: {
    id: string
    problemId: string
    language: string
    submissionId: string
    otherSubmissionId: string
    userId: string | null
    otherUserId: string | null
    score: number
    status: 'OPEN' | 'CONFIRMED' | 'DISMISSED'
    createdAt: string
  }
  source: string | null
  otherSource: string | null
}

export type SanctionKind = 'WARNING' | 'MUTE' | 'SUSPEND'

export interface Sanction {
  id: string
  userId: string
  kind: SanctionKind
  reason: string
  evidence: string
  issuedBy: string
  startsAt: string
  endsAt: string | null
  liftedAt: string | null
  appeal: string | null
  appealedAt: string | null
  appealResolution: 'UPHELD' | 'LIFTED' | null
  appealNote: string | null
  createdAt: string
}

export interface RoleGrant {
  userId: string
  role: AdminRole
  grantedBy: string
  grantedAt: string
}

export interface GrantRequest {
  id: string
  userId: string
  role: AdminRole
  reason: string
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED'
  requestedBy: string
  createdAt: string
}

export interface AuditEntry {
  id: number
  action: string
  subject: string
  actor: string
  detail: string
  createdAt: string
}

/* ─── 호출 ─── */

export const adminApi = {
  me: () => admin<Me>('/me'),

  pendingVersions: () => admin<PendingVersion[]>('/problems/versions/pending'),
  publish: (problemId: string, version: number, reportDigest: string, validatorVersion: string) =>
    post<{ publishedVersionId: string }>(`/problems/${problemId}/publish`, { version, reportDigest, validatorVersion }),

  rejudges: () => admin<RejudgeJob[]>('/rejudges?limit=50'),
  rejudgeReport: (id: string) => admin<RejudgeReport>(`/rejudges/${id}`),
  requestRejudge: (scope: string, reason: string, dryRun: boolean) => post<RejudgeJob>('/rejudges', { scope, reason, dryRun }),
  approveRejudge: (id: string) => post<RejudgeJob>(`/rejudges/${id}/approve`),
  rejectRejudge: (id: string, reason: string) => post<RejudgeJob>(`/rejudges/${id}/reject`, { reason }),
  dispatchRejudge: (id: string) => post<{ targets: number }>(`/rejudges/${id}/dispatch`),

  discussionQueue: () => admin<ReportedPost[]>('/discussions/queue'),
  resolveDiscussion: (reportId: string, hide: boolean, resolution: string) =>
    post(`/discussions/reports/${reportId}/resolve`, { hide, resolution }),

  arenaQueue: () => admin<ArenaQueue>('/arena/queue'),
  approveDonation: (id: string, kind: string, note: string | null) => post(`/arena/donations/${id}/approve`, { kind, note }),
  rejectDonation: (id: string, reason: string) => post(`/arena/donations/${id}/reject`, { reason }),
  resolveArenaReport: (reportId: string, retire: boolean, resolution: string) =>
    post(`/arena/reports/${reportId}/resolve`, { retire, resolution }),

  integrityQueue: () => admin<FlaggedPair[]>('/integrity/queue'),
  resolveFlag: (id: string, confirmed: boolean, note: string | null) => post(`/integrity/flags/${id}/resolve`, { confirmed, note }),

  appeals: () => admin<Sanction[]>('/sanctions/appeals'),
  sanctionHistory: (userId: string) => admin<Sanction[]>(`/sanctions/users/${encodeURIComponent(userId)}`),
  issueSanction: (request: { userId: string; kind: SanctionKind; reason: string; evidence: string; days: number | null }) =>
    post<Sanction>('/sanctions', request),
  liftSanction: (id: string) => post<Sanction>(`/sanctions/${id}/lift`),
  resolveAppeal: (id: string, uphold: boolean, note: string | null) => post<Sanction>(`/sanctions/${id}/appeal/resolve`, { uphold, note }),

  operators: () => admin<RoleGrant[]>('/operators'),
  roleRequests: () => admin<GrantRequest[]>('/role-requests'),
  requestRole: (userId: string, role: AdminRole, reason: string) =>
    post<{ status: 'REQUESTED' | 'GRANTED' | 'UNCHANGED' }>(`/operators/${encodeURIComponent(userId)}/roles`, { role, reason }),
  approveRole: (requestId: string) => post(`/role-requests/${requestId}/approve`),
  rejectRole: (requestId: string, reason: string) => post(`/role-requests/${requestId}/reject`, { reason }),
  revokeRole: (userId: string, role: AdminRole) =>
    admin(`/operators/${encodeURIComponent(userId)}/roles/${role}`, { method: 'DELETE' }),

  audit: (subject: string | null) =>
    admin<AuditEntry[]>(`/audit?limit=100${subject ? `&subject=${encodeURIComponent(subject)}` : ''}`),
}
