package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * 역량 진단 (기획서 §4.2, PRD FR-801·FR-806).
 *
 * 증거를 받아 쌓고, 물어보면 그때 계산해 답한다. **숙련도를 저장하지 않는 이유**는
 * V19 마이그레이션에 적었다.
 *
 * 이 모듈은 다른 도메인 모듈을 참조하지 않는다 (§3.1). 증거는 조립 지점이 밀어 넣으며,
 * 그래서 이 클래스는 제출도 사전 질문도 모른 채 "무엇이 성공이었나"만 받는다.
 */
@Service
class CompetencyService(
    private val repository: EvidenceRepository,
    private val packages: ProblemPackageLoader,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 판정된 제출을 증거로 삼는다.
     *
     * 어느 역량에 붙는지는 **문제의 카탈로그가 정한다.** 역량 태그가 비어 있으면 아무
     * 증거도 만들지 않는다 — 그 경우 이 문제를 아무리 풀어도 숙련도가 움직이지 않으며,
     * 그것이 카탈로그에서 역량을 필수로 둔 이유다.
     *
     * 재채점으로 바뀐 판정은 넣지 않는다. 그때 달라진 것은 사용자의 능력이 아니라 문제
     * 데이터이고, 그것을 증거로 세면 남이 테스트를 고쳤다고 내 숙련도가 움직인다.
     */
    fun judged(userId: String, problemId: String, submissionId: String, accepted: Boolean) {
        val competencies = competenciesOf(problemId) ?: return
        record(
            userId, competencies, EvidenceSource.SUBMISSION, accepted, problemId,
            reference = submissionId,
            detail = if (accepted) "제출이 통과했다" else "제출이 통과하지 못했다",
        )
    }

    /**
     * 풀이 전 질문 응답 (FR-803).
     *
     * 어느 역량인지는 부르는 쪽이 정한다 — 질문의 종류가 곧 역량이고, 그 대응은 질문을
     * 만드는 쪽(Workspace)이 안다.
     */
    fun answered(
        userId: String,
        problemId: String,
        competency: Competency,
        correct: Boolean,
        reference: String,
        detail: String?,
    ) = record(userId, listOf(competency), EvidenceSource.PREQUESTION, correct, problemId, reference, detail)

    /**
     * 사용자가 직접 만든 테스트 (부록 A 실행 도메인).
     *
     * **성공을 "테스트가 통과했다"로 세지 않는다.** 여기서 재는 것은 코드가 맞았는지가
     * 아니라 **무엇을 시험해야 하는지 아는가**이므로(§8.2 테스트 설계·엣지케이스), 기대
     * 출력을 적어 스스로 채점한 케이스가 있으면 성공으로 본다. 입력만 넣고 출력을 구경한
     * 것은 시험이 아니라 실행이다.
     */
    fun tested(userId: String, problemId: String, trialId: String, judgedCases: Int) =
        record(
            userId, listOf(Competency.TEST_DESIGN), EvidenceSource.TRIAL,
            success = judgedCases > 0,
            problemId = problemId,
            reference = trialId,
            detail = if (judgedCases > 0) {
                "기대 출력을 적은 케이스 ${judgedCases}건을 돌렸다"
            } else {
                "기대 출력 없이 돌려만 봤다"
            },
        )

    /** 역량 지도 (FR-806). 증거가 없는 역량도 함께 낸다 — 미측정을 말할 수 있어야 한다. */
    fun mapOf(userId: String): List<Mastery> = MasteryProjection.of(repository.of(userId))

    /** 한 역량의 근거. 화면이 여기서 문제와 제출로 내려간다. */
    fun evidenceOf(userId: String, competency: Competency, limit: Int = 20): List<Evidence> =
        repository.of(userId, competency, limit)

    private fun competenciesOf(problemId: String): List<Competency>? =
        runCatching { packages.load(problemId).catalog.competencies }
            .onFailure { log.warn("문제를 읽지 못해 증거를 만들지 않는다: {} ({})", problemId, it.message) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }

    private fun record(
        userId: String,
        competencies: List<Competency>,
        source: EvidenceSource,
        success: Boolean,
        problemId: String,
        reference: String?,
        detail: String?,
    ) {
        val now = Instant.now()
        for (competency in competencies) {
            repository.insert(
                Evidence(
                    id = UUID.randomUUID(),
                    userId = userId,
                    competency = competency,
                    source = source,
                    success = success,
                    // 도움 기능이 아직 없어 전부 1.0 이다. 붙으면 여기가 낮아진다 (§3.4).
                    weight = 1.0,
                    problemId = problemId,
                    reference = reference,
                    detail = detail,
                    occurredAt = now,
                ),
            )
        }
    }
}
