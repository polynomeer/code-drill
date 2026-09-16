package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.DefectKind
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.ProjectPackageLoader
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
    /** 프로젝트형 문제의 카탈로그 (11단계). null 이면 프로젝트 판정은 증거가 되지 않는다 — 프로젝트 없는 조립뿐이다. */
    private val projects: ProjectPackageLoader? = null,
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
    fun judged(
        userId: String,
        problemId: String,
        submissionId: String,
        accepted: Boolean,
        helpLevel: Int = 0,
    ) {
        val competencies = competenciesOf(problemId) ?: return
        record(
            userId, competencies, EvidenceSource.SUBMISSION, accepted, problemId,
            reference = submissionId,
            detail = if (accepted) "제출이 통과했다" else "제출이 통과하지 못했다",
            weight = weightFor(helpLevel),
        )
    }

    /**
     * 프로젝트형 판정을 증거로 삼는다 (11단계).
     *
     * 어느 역량에 붙는지는 프로젝트의 카탈로그가 정하고, 그것은 실무군뿐이다 — 알고리즘
     * 증거와 한 칸에 섞이지 않는다. 사용자가 시작 저장소보다 테스트를 더 썼으면 테스트 작성의
     * 증거가 하나 더 선다: 더한 테스트가 자기 제출에서 통과했는가.
     *
     * 도움 단계는 없다. 프로젝트형에는 아직 코칭이 붙지 않았고, 붙으면 알고리즘 제출과 같은
     * 무게 표를 쓴다.
     */
    fun projectJudged(
        userId: String,
        projectId: String,
        submissionId: String,
        accepted: Boolean,
        addedTests: Int,
        addedTestsPassed: Boolean,
    ) {
        val competencies = projects?.let { loader ->
            runCatching { loader.load(projectId).catalog.competencies }.getOrNull()
        }?.takeIf { it.isNotEmpty() } ?: return
        record(
            userId, competencies, EvidenceSource.PROJECT, accepted, projectId,
            reference = submissionId,
            detail = if (accepted) "숨은 스위트를 전부 통과했다" else "숨은 스위트를 다 통과하지 못했다",
        )
        if (addedTests > 0) {
            record(
                userId, listOf(Competency.TEST_WRITING), EvidenceSource.PROJECT_TESTS, addedTestsPassed, projectId,
                reference = submissionId,
                detail = if (addedTestsPassed) "테스트 ${addedTests}개를 더 썼고 통과했다" else "테스트 ${addedTests}개를 더 썼는데 통과하지 못했다",
            )
        }
    }

    /**
     * 도움 단계 → 증거 무게 (PRD §3.4, FR-806).
     *
     * > 표본 부족·힌트 사용·AI 도움을 신뢰도와 증거 가중치에 반영합니다.
     *
     * **0 으로 내리지 않는다.** 힌트를 보고 푼 것도 못 푼 것보다는 아는 것이며, 0 으로
     * 두면 도움을 받은 순간 그 문제는 아무리 잘 풀어도 없던 일이 된다 — 그러면 사용자는
     * 막혀도 힌트를 누르지 않고, 코칭 기능이 있으나 마나가 된다.
     *
     * 깊은 단계일수록 가파르게 떨어진다. 방향만 잡아 준 1단계와 방법을 말해 준 3단계는
     * 남은 몫이 전혀 다르다.
     *
     * 경계값은 저작자 판단이며, 바꾸면 지난 증거를 그대로 두고 다시 계산하면 된다 —
     * 그러라고 무게를 증거에 박아 저장한다.
     */
    private fun weightFor(helpLevel: Int): Double = when {
        helpLevel <= 0 -> 1.0
        helpLevel == 1 -> 0.7
        helpLevel == 2 -> 0.45
        else -> 0.3
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

    /**
     * 사용자의 테스트를 대표 오답에 겨눈 결과 (FR-804).
     *
     * **역량마다 한 줄씩만 남긴다.** 결함군마다 남기고 싶었는데, 경계 어긋남과 빠진 경계
     * 입력이 둘 다 [Competency.EDGE_CASES] 로 가는 바람에 한 평가가 같은 (사용자, 역량,
     * 출처, 참조) 로 두 번 들어갔고, V19 의 유일 색인이 **말없이 하나를 버렸다.** 남은
     * 것이 어느 쪽인지도 정해져 있지 않았다.
     *
     * 그 색인은 지우면 안 되는 것이다 — 아웃박스가 at-least-once 라, 없으면 재전달 하나가
     * 증거 두 개가 되어 숙련도가 실제보다 단단해 보인다. 그래서 색인을 고치는 대신
     * **역량으로 먼저 접는다.** 한 평가는 한 역량에 한 사건이고, 그것이 사실이다.
     *
     * 손으로 잡을 수 없는 결함군([DefectKind.reachableByHandWrittenCase])은 세지 않는다.
     * 성능 오답은 한도를 넘길 만큼 큰 입력이 있어야 잡히고, 그런 입력은 테스트 패널에
     * 손으로 적을 수 있는 것이 아니다 — 못 잡았다고 역량을 깎으면 사용자가 고칠 수 없는
     * 것으로 벌하는 셈이다.
     */
    fun mutationChecked(
        userId: String,
        problemId: String,
        evaluationId: String,
        killedByKind: Map<DefectKind, Pair<Int, Int>>,
    ) {
        val scored = killedByKind.filterKeys { it.reachableByHandWrittenCase }
        if (scored.isEmpty()) return

        // Map 이라 같은 역량이 두 번 나올 수 없다. 위의 사고를 주석이 아니라 타입이 막는다.
        val byCompetency: Map<Competency, Pair<Int, Int>> = scored.entries
            .mapNotNull { (kind, counts) -> kind.competency()?.let { it to counts } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, rows) -> rows.sumOf { it.first } to rows.sumOf { it.second } }

        for ((competency, counts) in byCompetency) {
            val (killed, total) = counts
            record(
                userId, listOf(competency), EvidenceSource.MUTATION,
                // **전부 잡아야 성공이다.** 한 역량에 묶인 오답들은 같은 종류의 실수를 다른
                // 자리에서 낸 것이라, 하나만 잡고 나머지를 놓쳤다면 그 종류를 아는 것이
                // 아니라 그 자리를 우연히 짚은 것이다.
                success = killed == total,
                problemId = problemId,
                reference = evaluationId,
                detail = "관련 오답 ${total}개 중 ${killed}개를 잡았다",
            )
        }

        // 전체는 따로 한 줄 남긴다. 역량별 증거만 남기면 "무엇을 시험해야 하는지 아는가"를
        // 통째로 재는 줄이 없어진다. 위 대응에서 TEST_DESIGN 으로 가는 결함군이 없으므로
        // 이 줄은 방금 넣은 것들과 부딪히지 않는다.
        val killed = scored.values.sumOf { it.first }
        val total = scored.values.sumOf { it.second }
        record(
            userId, listOf(Competency.TEST_DESIGN), EvidenceSource.MUTATION,
            success = killed.toDouble() / total >= KILL_TARGET,
            problemId = problemId,
            reference = evaluationId,
            detail = "오답 ${total}개 중 ${killed}개를 잡는 테스트를 적었다",
        )
    }

    /**
     * 코칭 후 힌트 없이 변형 문제를 통과했다 (FR-807).
     *
     * > 힌트·AI 없이 완료한 결과를 가장 높은 가중치의 증거로 반영합니다.
     *
     * **1.0 보다 무겁다.** 다른 증거와 같은 무게로 세면 "가장 높은 가중치"라는 말이
     * 아무 뜻도 갖지 못한다. 숙련도는 가중 성공률이라 1 을 넘는 무게가 그대로 통한다.
     *
     * [Competency.TRANSFER] 에도 한 줄 남긴다. 옮길 줄 아는 것 자체가 역량이고
     * (§4.2 확장군), 그것을 재는 사건은 이것뿐이다.
     */
    fun transferred(
        userId: String,
        problemId: String,
        taskId: String,
        competencies: List<Competency>,
    ) {
        val targets = (competencies + Competency.TRANSFER).distinct()
        record(
            userId, targets, EvidenceSource.TRANSFER,
            success = true,
            problemId = problemId,
            reference = taskId,
            detail = "코칭 뒤 힌트 없이 변형 문제를 통과했다",
            weight = TRANSFER_WEIGHT,
        )
    }

    /**
     * 리플레이 중 다음 상태 예측 (FR-805).
     *
     * [Competency.DEBUGGING] 의 증거다 — 여기서 재는 것은 답이 맞았는가가 아니라
     * **자기 코드가 다음에 무엇을 할지 아는가**이고, 그것을 모르면 어디서부터 어긋났는지도
     * 짚을 수 없다 (§4.2 검증군).
     *
     * 무게가 낮은 이유는 [EvidenceSource.PREDICTION] 에 적었다.
     */
    fun predicted(userId: String, problemId: String, predictionId: String, correct: Boolean) =
        record(
            userId, listOf(Competency.DEBUGGING), EvidenceSource.PREDICTION,
            success = correct,
            problemId = problemId,
            reference = predictionId,
            detail = if (correct) "다음 이벤트를 맞혔다" else "다음 이벤트를 맞히지 못했다",
            weight = PREDICTION_WEIGHT,
        )

    /**
     * 아레나에서 오답을 깨뜨려 봤다 (§8.3, §8.2 "깨뜨린 제출과 반례 품질").
     *
     * [Competency.COUNTEREXAMPLE] 의 증거다. 하나라도 깨뜨리면 성공이다 — 반례는 하나면
     * 충분하고, 한 입력이 여럿을 깨뜨린 것은 더 좋은 반례이지 더 많은 성공이 아니다.
     */
    fun brokeMutants(userId: String, problemId: String, attemptId: String, broken: Int, total: Int) =
        record(
            userId, listOf(Competency.COUNTEREXAMPLE), EvidenceSource.ARENA,
            success = broken > 0,
            problemId = problemId,
            reference = attemptId,
            detail = if (broken > 0) "오답 ${total}개 중 ${broken}개를 깨뜨리는 입력을 적었다" else "오답 ${total}개 중 하나도 깨뜨리지 못했다",
        )

    /**
     * 역량 지도 (FR-806). 증거가 없는 역량도 함께 낸다 — 미측정을 말할 수 있어야 한다.
     *
     * [asOf] 를 주면 그 시점까지의 증거로 그린다. 숙련도를 저장하지 않고 계산하기 때문에
     * 되는 일이고, 주간 리포트가 "지난주보다"를 말하는 방법이다.
     */
    fun mapOf(userId: String, asOf: Instant = Instant.now()): List<Mastery> =
        MasteryProjection.of(repository.of(userId), asOf)

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
        weight: Double = 1.0,
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
                    // 코칭에서 힌트를 본 제출은 여기가 낮아진다 (§3.4, [weightFor]).
                    weight = weight,
                    problemId = problemId,
                    reference = reference,
                    detail = detail,
                    occurredAt = now,
                ),
            )
        }
    }

    private companion object {
        /**
         * 테스트 설계를 "할 줄 안다"고 볼 경계.
         *
         * 100% 로 두지 않는다. 저작 게이트는 저작자의 테스트에 100% 를 요구하지만(§6.3),
         * 저쪽은 오답 목록을 보고 쓴 테스트이고 이쪽은 문제만 보고 쓴 테스트다. 같은
         * 잣대를 대면 사실상 아무도 넘지 못하고, 넘지 못하는 경계는 아무것도 가르지 않는다.
         *
         * 저작자 판단이며, 바꾸면 지난 증거를 그대로 두고 다시 계산하면 된다.
         */
        const val KILL_TARGET = 0.75

        /**
         * 전이 증거의 무게 (FR-807 "가장 높은 가중치").
         *
         * 이것 하나가 도움 없이 푼 제출 한 번 반이다. 그만큼 드물고 그만큼 많은 것을
         * 말해 주기 때문이다 — 코칭받은 문제를 다시 푸는 것은 기억일 수 있지만, 힌트
         * 없이 다른 문제를 푸는 것은 기억으로는 되지 않는다.
         */
        const val TRANSFER_WEIGHT = 1.5

        /**
         * 예측 증거의 무게.
         *
         * 낮다. 트레이스가 이미 클라이언트에 있어 답을 보고 누를 수 있기 때문이며,
         * 그 구멍은 막을 수 없다 — 자기 실행의 기록을 자기에게 숨길 수는 없다. 그래도
         * 0 으로 두지는 않는다: 스스로 속이지 않은 사람의 기록까지 버릴 이유는 없다.
         */
        const val PREDICTION_WEIGHT = 0.5
    }
}

/**
 * 결함군 → 역량 (기획서 §4.2).
 *
 * 대응을 여기 두는 이유는 [MasteryProjection] 과 같다 — **무엇이 무엇의 증거인가는
 * 역량 쪽의 판단이다.** 결함을 만든 쪽(콘텐츠)은 그 오답이 어떤 실수인지만 알면 된다.
 *
 * 재지 않는 결함군은 `null` 이다. 아무 역량으로나 떨어뜨리지 않는 이유는 그 역량이
 * [Competency.TEST_DESIGN] 일 수밖에 없는데, 거기에는 이미 전체 결과 한 줄이 들어가고
 * 둘은 같은 참조를 갖는다 — V19 의 유일 색인이 그중 하나를 말없이 버린다.
 */
private fun DefectKind.competency(): Competency? = when (this) {
    // 둘 다 "경계에서 무너지는 자리"를 짚었는지를 묻는다. 하나 어긋난 인덱스와 빈 입력은
    // 겉모습이 다를 뿐 같은 자리를 못 본 것이다.
    DefectKind.OFF_BY_ONE -> Competency.EDGE_CASES
    DefectKind.MISSING_EDGE_CASE -> Competency.EDGE_CASES

    // 반례다. 대부분의 입력에서 맞고 일부에서만 틀리는 구현을 넘어뜨리려면, 그 "일부"를
    // 짚어 내는 입력을 만들어야 한다.
    DefectKind.WRONG_BRANCH -> Competency.COUNTEREXAMPLE
    DefectKind.WRONG_ALGORITHM -> Competency.COUNTEREXAMPLE

    DefectKind.PERFORMANCE -> null
    DefectKind.UNSPECIFIED -> null
}
