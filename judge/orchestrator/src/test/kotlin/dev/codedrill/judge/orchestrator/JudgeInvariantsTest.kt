package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.aggregation.VerdictAggregator
import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Aggregation
import dev.codedrill.platform.problempackage.GroupPolicy
import dev.codedrill.platform.problempackage.StopPolicy
import dev.codedrill.platform.problempackage.Visibility
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * §16.2 의 성공 기준: **중복 전달, 워커 유실, 트레이스 실패에도 판정 불변식이 유지된다.**
 *
 * 이 테스트가 그 기준을 고정한다.
 */
class JudgeInvariantsTest {

    // --- 집계 (§4.1, §6.2) ---

    @Test
    fun `모든 케이스가 통과하면 만점과 ACCEPTED 다`() {
        val result = VerdictAggregator.aggregate(POLICIES, resultOf(allPassing()))

        assertEquals(Verdict.ACCEPTED, result.verdict)
        assertEquals(100, result.score)
    }

    @Test
    fun `ALL_OR_NOTHING 그룹은 한 건만 틀려도 0 점이다`() {
        val cases = allPassing().map {
            if (it.caseId == "b2") it.copy(verdict = Verdict.WRONG_ANSWER) else it
        }

        val result = VerdictAggregator.aggregate(POLICIES, resultOf(cases))

        assertEquals(Verdict.WRONG_ANSWER, result.verdict)
        assertEquals(0, result.groups.first { it.groupId == "boundary" }.score)
        assertEquals(60, result.score, "다른 그룹 점수는 남아야 한다")
    }

    @Test
    fun `SUM 그룹은 통과 비율만큼 부분 점수를 준다`() {
        val policies = listOf(POLICIES[0], POLICIES[1].copy(aggregation = Aggregation.SUM), POLICIES[2])
        val cases = allPassing().map {
            if (it.caseId == "b2") it.copy(verdict = Verdict.WRONG_ANSWER) else it
        }

        val result = VerdictAggregator.aggregate(policies, resultOf(cases))

        assertEquals(20, result.groups.first { it.groupId == "boundary" }.score, "2건 중 1건 = 40의 절반")
    }

    @Test
    fun `플랫폼 장애는 오답으로 덮이지 않는다`() {
        val cases = allPassing().map {
            when (it.caseId) {
                "b2" -> it.copy(verdict = Verdict.WRONG_ANSWER)
                "h1" -> it.copy(verdict = Verdict.SYSTEM_ERROR)
                else -> it
            }
        }

        val result = VerdictAggregator.aggregate(POLICIES, resultOf(cases))

        assertEquals(Verdict.SYSTEM_ERROR, result.verdict, "SYSTEM_ERROR 는 별도 축이다 (§4.4)")
    }

    @Test
    fun `실행되지 않은 그룹은 통과로 처리하지 않는다`() {
        // TIME_LIMIT 으로 중단돼 hidden 그룹 결과가 아예 없는 상황.
        val cases = allPassing().filter { it.groupId != "hidden" }

        val result = VerdictAggregator.aggregate(POLICIES, resultOf(cases))

        assertEquals(Verdict.SYSTEM_ERROR, result.verdict)
    }

    @Test
    fun `집계는 몇 번을 돌려도 같은 값을 낸다`() {
        val execution = resultOf(allPassing().shuffled())

        val first = VerdictAggregator.aggregate(POLICIES, execution)
        val second = VerdictAggregator.aggregate(POLICIES, execution)

        assertEquals(first, second)
    }

    @Test
    fun `컴파일 실패는 케이스 없이 그대로 최종 판정이 된다`() {
        val execution = resultOf(emptyList()).copy(
            terminalVerdict = Verdict.COMPILE_ERROR,
            compileLog = "(3:5) expecting an element",
        )

        val result = VerdictAggregator.aggregate(POLICIES, execution)

        assertEquals(Verdict.COMPILE_ERROR, result.verdict)
        assertEquals(0, result.score)
        assertEquals("(3:5) expecting an element", result.compileLog)
    }

    // --- 임대와 fencing (§4.3) ---

    @Test
    fun `같은 결과가 다시 오면 no-op 이다`() {
        val registry = AttemptRegistry()
        val lease = registry.lease(SUBMISSION)
        val result = resultOf(allPassing(), lease.attempt, lease.token)

        assertIs<AttemptRegistry.Acceptance.Accepted>(registry.accept(result))
        assertIs<AttemptRegistry.Acceptance.Duplicate>(registry.accept(result))
    }

    @Test
    fun `워커 유실 후 살아 돌아온 결과는 거절한다`() {
        val registry = AttemptRegistry()
        val lost = registry.lease(SUBMISSION)
        val staleResult = resultOf(allPassing(), lost.attempt, lost.token)

        // 임대가 만료돼 재임대했다. 이 순간 이전 임대는 무효다.
        val current = registry.lease(SUBMISSION)
        assertTrue(current.token > lost.token, "재임대는 토큰을 올려야 한다")
        assertEquals(2, current.attempt)

        val acceptance = registry.accept(staleResult)

        val stale = assertIs<AttemptRegistry.Acceptance.Stale>(acceptance)
        assertTrue(stale.reason.contains("fencing"), "거절 사유: ${stale.reason}")
    }

    @Test
    fun `재임대 후 새 워커의 결과는 받아들인다`() {
        val registry = AttemptRegistry()
        registry.lease(SUBMISSION)
        val current = registry.lease(SUBMISSION)

        val acceptance = registry.accept(resultOf(allPassing(), current.attempt, current.token))

        assertIs<AttemptRegistry.Acceptance.Accepted>(acceptance)
    }

    @Test
    fun `종료된 제출에 다른 결과가 오면 감사 대상이다`() {
        val registry = AttemptRegistry()
        val lease = registry.lease(SUBMISSION)
        registry.accept(resultOf(allPassing(), lease.attempt, lease.token))

        val different = resultOf(allPassing(), lease.attempt, lease.token).copy(resultDigest = "다른-digest")

        assertIs<AttemptRegistry.Acceptance.AlreadyCompleted>(registry.accept(different))
    }

    @Test
    fun `임대는 워커가 집어 든 뒤부터 시간을 잰다`() {
        val start = Instant.parse("2026-09-05T00:00:00Z")
        var now = start
        val registry = AttemptRegistry(
            clock = object : Clock() {
                override fun getZone() = ZoneOffset.UTC
                override fun withZone(zone: java.time.ZoneId) = this
                override fun instant() = now
            },
            leaseDuration = Duration.ofSeconds(30),
            dispatchTimeout = Duration.ofMinutes(5),
        )

        val lease = registry.lease(SUBMISSION)
        assertTrue(!registry.isExpired(SUBMISSION), "방금 임대한 실행은 살아 있다")

        // 아직 아무도 집어 들지 않았다. 이 구간은 큐 대기이지 워커 유실이 아니다.
        now = start.plusSeconds(31)
        assertTrue(!registry.isExpired(SUBMISSION), "큐에서 기다린 시간은 임대를 소모하지 않는다")

        // 워커가 집어 들었다. 이제부터 만료는 "워커가 죽었다"를 뜻한다.
        registry.renew(SUBMISSION, lease.token)
        now = start.plusSeconds(62)
        assertTrue(registry.isExpired(SUBMISSION), "심장 박동이 끊기면 회수 대상이다")
    }

    // --- 픽스처 ---

    private fun allPassing() = listOf(
        case("s1", "sample"), case("s2", "sample"),
        case("b1", "boundary"), case("b2", "boundary"),
        case("h1", "hidden"), case("h2", "hidden"),
    )

    private fun case(id: String, group: String) =
        TestCaseResult(id, group, Verdict.ACCEPTED, Measurements.NONE)

    private fun resultOf(
        cases: List<TestCaseResult>,
        attempt: Int = 1,
        token: FencingToken = FencingToken(1),
    ) = ExecutionResult(
        executionId = "exec-1",
        submissionId = SUBMISSION,
        attempt = attempt,
        fencingToken = token,
        terminalVerdict = null,
        compileLog = null,
        cases = cases,
        resultDigest = "digest-" + cases.sortedBy { it.caseId }.joinToString { "${it.caseId}=${it.verdict}" },
    )

    private companion object {
        const val SUBMISSION = "sub-1"

        val POLICIES = listOf(
            GroupPolicy("sample", 0, Visibility.PUBLIC, Aggregation.ALL_OR_NOTHING, StopPolicy.FAIL_FAST),
            GroupPolicy("boundary", 40, Visibility.HIDDEN, Aggregation.ALL_OR_NOTHING, StopPolicy.CONTINUE),
            GroupPolicy("hidden", 60, Visibility.HIDDEN, Aggregation.ALL_OR_NOTHING, StopPolicy.CONTINUE),
        )
    }
}
