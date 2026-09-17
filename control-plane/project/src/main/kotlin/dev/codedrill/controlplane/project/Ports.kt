package dev.codedrill.controlplane.project

import dev.codedrill.judge.protocol.WorkspaceRef

/**
 * 공개된 콘텐츠 id. Admin 이 소유하는 상태이고, 프로젝트형 문제도 같은 표에 등록·공개된다 —
 * 프로젝트형 문제도 문제다 (§3.2). 조립 지점이 잇는다.
 */
fun interface PublishedProjects {
    fun ids(): Set<String>
}

/**
 * 워크스페이스가 실행 영역으로 가는 길 (§8.3, production-readiness B3).
 *
 * 제출의 파일들은 DB 에 남고(화면·반출의 원본), 스토어의 것은 실행용 복제다. 메시지에는
 * 참조와 digest 만 실린다. Submission 모듈의 SourceStore 와 같은 분담이다.
 */
interface WorkspaceStore {
    fun store(submissionId: String, files: Map<String, String>): WorkspaceRef
    /** 있고 digest 가 맞으면 그대로, 아니면 다시 올린다 — 재채점의 길이다. */
    fun ensure(submissionId: String, files: Map<String, String>): WorkspaceRef
    fun delete(submissionId: String)
}

/**
 * 판정을 역량 증거로 잇는 창구 (§3.1 조립 지점, 11단계). Project 는 Competency 를 모른다.
 *
 * [addedTests] 는 사용자가 시작 저장소보다 더 쓴 테스트 메서드 수, [addedTestsPassed] 는
 * 그것들이 — 공개 테스트 전부가 — 자기 제출에서 통과했는가다.
 */
fun interface ProjectLearningSignals {
    /** [score] 와 [submittedAt] 은 대회의 것이다 — 대회 중의 판정은 그 대회의 점수이고, 창은 제출 시각으로 본다 (§8.4). */
    fun judged(
        userId: String,
        projectId: String,
        submissionId: String,
        accepted: Boolean,
        score: Int,
        submittedAt: java.time.Instant,
        addedTests: Int,
        addedTestsPassed: Boolean,
    )

    companion object {
        val NONE = ProjectLearningSignals { _, _, _, _, _, _, _, _ -> }
    }
}

/**
 * 이 판정이 재채점의 결과인지, 그렇다면 어떻게 다뤄야 하는지 (§3.1 조립 지점).
 *
 * Submission 모듈의 RejudgeContext 와 같은 모양이다. 타입을 공유하면 두 모듈이 서로를 참조하게
 * 되므로 여기 하나 더 두고, 조립 지점이 Admin 의 것으로 잇는다. 기본은 [NONE] — 재채점이 없는
 * 조립에서도 최초 판정은 정상적으로 반영돼야 한다.
 */
interface ProjectRejudgeContext {
    fun pendingFor(submissionId: java.util.UUID): Pending?
    fun judged(outcome: Outcome)

    data class Pending(val jobId: java.util.UUID, val dryRun: Boolean)

    data class Outcome(
        val submissionId: java.util.UUID,
        val jobId: java.util.UUID,
        val applied: Boolean,
        val previousVerdict: String?,
        val previousScore: Int?,
        val verdict: String,
        val score: Int,
    )

    companion object {
        val NONE = object : ProjectRejudgeContext {
            override fun pendingFor(submissionId: java.util.UUID): Pending? = null
            override fun judged(outcome: Outcome) = Unit
        }
    }
}
