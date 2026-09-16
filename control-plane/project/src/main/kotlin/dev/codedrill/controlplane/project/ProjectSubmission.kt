package dev.codedrill.controlplane.project

import dev.codedrill.judge.protocol.ProjectTestOutcome
import dev.codedrill.judge.protocol.Verdict
import java.time.Instant
import java.util.UUID

/**
 * 프로젝트형 제출 (feature-roadmap 11단계).
 *
 * 상태는 셋이다 — `QUEUED → LEASED → COMPLETED`. 알고리즘 제출의 COMPILING·RUNNING·
 * AGGREGATING 이 없는 것은 빌드 한 번에 스위트 한 번이라 중간 단계를 보여 줄 것이 없기
 * 때문이다. 종착은 불변이다 (§4.2).
 */
data class ProjectSubmission(
    val id: UUID,
    val userId: String,
    /** 저장에만 쓴다. 응답에는 나가지 않는다 (§4.3 제출 버튼 중복). */
    val idempotencyKey: String,
    val projectId: String,
    val projectVersion: Int,
    val language: String,
    val status: Status,
    val verdict: Verdict? = null,
    val score: Int? = null,
    val log: String? = null,
    /** 공개 테스트의 결과. 숨은 것은 [hiddenPassed]/[hiddenTotal] 뿐이다 (§8.3). */
    val tests: List<ProjectTestOutcome> = emptyList(),
    val hiddenPassed: Int? = null,
    val hiddenTotal: Int? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val version: Int = 0,
) {
    enum class Status {
        QUEUED, LEASED, COMPLETED;

        val terminal: Boolean get() = this == COMPLETED
    }
}

/** 화면이 보는 제출. [files] 는 상세에서만, 그리고 소유자에게만 실린다. */
data class ProjectSubmissionResponse(
    val id: String,
    val projectId: String,
    val projectVersion: Int,
    val language: String,
    val status: ProjectSubmission.Status,
    val verdict: Verdict?,
    val score: Int?,
    val log: String?,
    val tests: List<ProjectTestOutcome>,
    val hiddenPassed: Int?,
    val hiddenTotal: Int?,
    val createdAt: Instant,
    val completedAt: Instant?,
    val files: Map<String, String>? = null,
) {
    companion object {
        fun of(submission: ProjectSubmission, files: Map<String, String>? = null) = ProjectSubmissionResponse(
            id = submission.id.toString(),
            projectId = submission.projectId,
            projectVersion = submission.projectVersion,
            language = submission.language,
            status = submission.status,
            verdict = submission.verdict,
            score = submission.score,
            log = submission.log,
            tests = submission.tests,
            hiddenPassed = submission.hiddenPassed,
            hiddenTotal = submission.hiddenTotal,
            createdAt = submission.createdAt,
            completedAt = submission.completedAt,
            files = files,
        )
    }
}

/**
 * 작업 중인 초안 (§8.1). [version] 은 저장할 때마다 1 씩 오르고, 클라이언트가 마지막으로
 * 본 버전일 때만 덮어쓴다 — 두 탭에서 같은 프로젝트를 열어 둔 사용자가 파일을 잃지 않게.
 */
data class ProjectDraft(
    val userId: String,
    val projectId: String,
    val files: Map<String, String>,
    val version: Long,
    val updatedAt: Instant,
)
