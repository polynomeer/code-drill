package dev.codedrill.platform.observability

/**
 * 메트릭 이름과 태그 (기술 설계서 §13.2).
 *
 * **대시보드와 경보 규칙이 이 이름에 직접 묶인다.** 이름을 바꾸면 쿼리가 아무것도
 * 돌려주지 않고, 경보는 오류 없이 조용히 멈춘다 — 장애를 감지하지 못하는 가장 나쁜
 * 상태다. 그래서 이름을 상수로 모아 두고, 바꿀 때는 `deploy/observability/` 의 규칙과
 * 대시보드를 같은 커밋에서 고친다.
 *
 * 태그는 카디널리티가 낮은 것만 쓴다. 사용자 ID·제출 ID·실행 ID 는 태그가 아니라
 * 로그의 상관관계 필드로 남긴다 ([CorrelationIds]). 시계열 하나가 사용자 수만큼
 * 늘어나면 Prometheus 가 먼저 죽는다.
 */
object Metrics {

    // --- API 계층 (§12.1 Submit acceptance latency) ---

    /** Timer. API 수신 → submission/outbox 커밋. SLO p95 1초. */
    const val SUBMIT_ACCEPT = "codedrill.submission.accept"

    /** Counter. 태그 `result` = created | idempotent_hit (§9.1 idempotency hit). */
    const val SUBMISSION_CREATED = "codedrill.submission.created"

    // --- 큐 계층 (§12.1 Queue wait) ---

    /** Timer. QUEUED → LEASED. SLO p95 2초. */
    const val QUEUE_WAIT = "codedrill.queue.wait"

    /** Gauge. 아직 발행되지 않은 아웃박스 이벤트 수. 브로커 장애의 1차 신호다. */
    const val OUTBOX_PENDING = "codedrill.outbox.pending"

    /** Timer. 가장 오래된 미발행 이벤트의 나이(초). */
    const val OUTBOX_OLDEST_AGE = "codedrill.outbox.oldest.age"

    // --- 판정 품질 (§13.2 Judge Quality) ---

    /** Counter. 태그 `verdict`, `language`. 판정 분포가 급변하면 콘텐츠·런타임 배포를 본다. */
    const val VERDICT = "codedrill.verdict"

    /** Counter. 태그 `language`. SYSTEM_ERROR 는 사용자 실패와 다른 축이다 (§4.4). */
    const val SYSTEM_ERROR = "codedrill.system.error"

    /** Counter. 태그 `language`. 쿼터로 거절한 제출 (§10.2 남용 방어). */
    const val QUOTA_REJECTED = "codedrill.submission.quota.rejected"

    /** Counter. 보존 기간이 지나 지운 트레이스 (§7.4). */
    const val TRACE_RETENTION_DELETED = "codedrill.trace.retention.deleted"

    /**
     * Gauge(days). 가장 오래된 트레이스의 나이.
     *
     * 지운 수만 세면 정리가 멈춘 것과 지울 것이 없는 것이 같아 보인다. 이 값이 보존
     * 기간을 넘어 자라면 정리가 안 돌고 있는 것이다.
     */
    const val TRACE_OLDEST_AGE = "codedrill.trace.oldest.age"

    /** Counter. 태그 `outcome` = accepted | duplicate | already_completed | stale (§4.3). */
    const val RESULT_ACCEPTANCE = "codedrill.result.acceptance"

    /** Counter. 만료된 임대를 회수해 재실행한 횟수 (§4.3 워커 유실). */
    const val LEASE_RECLAIMED = "codedrill.lease.reclaimed"

    /** Timer. 종료 알림 수신 → 클라이언트 반영. SLO p95 1초. */
    const val VERDICT_PROPAGATION = "codedrill.verdict.propagation"

    /** Timer. 태그 `language`, `verdict`. 제출 → 종료까지 사용자가 실제로 기다린 시간. */
    const val SUBMISSION_DURATION = "codedrill.submission.duration"

    // --- Runner (§13.2 Runner) ---

    /** Timer. 태그 `language`, `outcome` = success | failure. */
    const val COMPILE = "codedrill.runner.compile"

    /** Timer. 태그 `language`, `mode`. 샌드박스 준비를 포함한 실행 시간. */
    const val EXECUTE = "codedrill.runner.execute"

    // --- 트레이스 (§13.2 Trace, §12.1 Trace manifest ready) ---

    /** Timer. 캡처 → manifest 준비. SLO p95 5초. */
    const val TRACE_PROCESS = "codedrill.trace.process"

    /** Counter. 태그 `outcome` = kept | dropped. dropped 가 많으면 예산이 작다 (§7.4). */
    const val TRACE_EVENTS = "codedrill.trace.events"

    /** Counter. 태그 `reason`. 검증에서 걸러진 트레이스 (§7.3). */
    const val TRACE_INVALID = "codedrill.trace.invalid"

    // --- 일관성 (§12.4) ---

    /** Gauge. 태그 `check`. 0 이 아니면 데이터가 어긋나 있다는 뜻이다. */
    const val CONSISTENCY_VIOLATIONS = "codedrill.consistency.violations"

    /** 태그 키. 오타로 태그가 갈라지지 않도록 상수로 둔다. */
    object Tag {
        const val LANGUAGE = "language"
        const val VERDICT = "verdict"
        const val OUTCOME = "outcome"
        const val REASON = "reason"
        const val RESULT = "result"
        const val MODE = "mode"
        const val CHECK = "check"
    }
}
