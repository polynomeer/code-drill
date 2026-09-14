package dev.codedrill.judge.orchestrator.lease

import dev.codedrill.judge.protocol.ExecutionResult
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.Verdict
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 임대와 fencing 의 계약 (기술 설계서 §4.3, §16.2 "중복 전달, 워커 유실에도 판정 불변식").
 *
 * 구현이 둘이고 둘 다 같은 답을 내야 한다. 메모리 구현은 테스트가 쓰고 Redis 구현은
 * 운영이 쓰므로, 한쪽만 검증하면 검증한 것과 도는 것이 다르다.
 */
abstract class LeaseRegistryContract {

    /** 시계를 손으로 돌린다. */
    protected var now: Instant = Instant.parse("2026-09-14T00:00:00Z")

    protected val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    protected val submission = "sub-${UUID.randomUUID()}"

    protected abstract fun registry(): LeaseRegistry

    @Test
    fun `같은 결과가 다시 오면 no-op 이다`() {
        val registry = registry()
        val lease = registry.lease(origin(), "exec-1")
        val result = resultOf(lease)

        assertIs<Acceptance.Accepted>(registry.accept(result))
        assertIs<Acceptance.Duplicate>(registry.accept(result))
    }

    @Test
    fun `워커 유실 후 살아 돌아온 결과는 거절한다`() {
        val registry = registry()
        val lost = registry.lease(origin(), "exec-1")
        val staleResult = resultOf(lost)

        // 임대가 만료돼 재임대했다. 이 순간 이전 임대는 무효다.
        val current = registry.lease(origin(), "exec-2")
        assertTrue(current.token > lost.token, "재임대는 토큰을 올려야 한다")
        assertEquals(2, current.attempt)

        val stale = assertIs<Acceptance.Stale>(registry.accept(staleResult))
        assertTrue(stale.reason.contains("fencing"), "거절 사유: ${stale.reason}")
    }

    @Test
    fun `재임대 후 새 워커의 결과는 받아들인다`() {
        val registry = registry()
        registry.lease(origin(), "exec-1")
        val current = registry.lease(origin(), "exec-2")

        val accepted = assertIs<Acceptance.Accepted>(registry.accept(resultOf(current)))
        assertEquals(submission, accepted.origin.submissionId, "임대가 품은 원 요청이 돌아온다")
    }

    @Test
    fun `종료된 제출에 다른 결과가 오면 감사 대상이다`() {
        val registry = registry()
        val lease = registry.lease(origin(), "exec-1")
        registry.accept(resultOf(lease))

        val different = resultOf(lease).copy(resultDigest = "다른-digest")

        assertIs<Acceptance.AlreadyCompleted>(registry.accept(different))
    }

    @Test
    fun `임대 없이 온 결과는 넘기되 그 사실을 남긴다`() {
        assertIs<Acceptance.Unleased>(registry().accept(resultOf(attempt = 1, token = FencingToken(99))))
    }

    @Test
    fun `임대는 워커가 집어 든 뒤부터 시간을 잰다`() {
        val registry = registry()
        val start = now
        val lease = registry.lease(origin(), "exec-1")
        assertTrue(registry.expired().none { it.submissionId == submission }, "방금 임대한 실행은 살아 있다")

        // 아직 아무도 집어 들지 않았다. 이 구간은 큐 대기이지 워커 유실이 아니다.
        now = start.plusSeconds(31)
        assertTrue(registry.expired().none { it.submissionId == submission }, "큐에서 기다린 시간은 임대를 소모하지 않는다")

        // 워커가 집어 들었다. 이제부터 만료는 "워커가 죽었다"를 뜻한다.
        assertTrue(registry.renew(submission, lease.token))
        now = start.plusSeconds(62)
        val expired = assertNotNull(registry.expired().singleOrNull { it.submissionId == submission }, "심장 박동이 끊기면 회수 대상이다")
        assertTrue(expired.started)
        assertEquals("exec-1", expired.executionId)
        assertEquals(origin(), expired.origin, "회수하는 쪽이 다시 걸 원 요청이 임대 안에 있다")
    }

    @Test
    fun `무효가 된 워커의 심장 박동은 임대를 살리지 못한다`() {
        val registry = registry()
        val lost = registry.lease(origin(), "exec-1")
        registry.lease(origin(), "exec-2")

        assertTrue(!registry.renew(submission, lost.token))
    }

    @Test
    fun `만료된 임대는 한 번만 회수된다`() {
        val registry = registry()
        val lease = registry.lease(origin(), "exec-1")
        registry.renew(submission, lease.token)
        now = now.plusSeconds(62)
        val expired = registry.expired().single { it.submissionId == submission }

        // 회수 인스턴스 둘이 같은 만료를 봤다. 먼저 바꾼 쪽만 이긴다.
        val won = assertNotNull(registry.reclaim(expired, "exec-2"))
        assertNull(registry.reclaim(expired, "exec-3"), "둘째는 물러난다 — 같은 제출을 두 번 걸지 않는다")

        assertEquals(2, won.attempt)
        assertTrue(won.token > expired.token)
        assertIs<Acceptance.Stale>(registry.accept(resultOf(expired)), "죽은 줄 알았던 워커의 결과")
        assertIs<Acceptance.Accepted>(registry.accept(resultOf(won)))
    }

    @Test
    fun `다시 임대하면 지난 판정과 내용이 같아도 받아들인다`() {
        val registry = registry()
        val first = registry.lease(origin(), "exec-1")
        registry.accept(resultOf(first).copy(resultDigest = "같은-내용"))

        // 재채점. 결과가 지난 판정과 같은 것은 재채점에서 흔한 일이다.
        val again = registry.lease(origin(), "exec-2")
        assertIs<Acceptance.Accepted>(registry.accept(resultOf(again).copy(resultDigest = "같은-내용")))
    }

    @Test
    fun `포기한 실행에 뒤늦게 온 결과는 덮어쓰지 못한다`() {
        val registry = registry()
        val lease = registry.lease(origin(), "exec-1")
        registry.abandon(submission)

        assertIs<Acceptance.AlreadyCompleted>(registry.accept(resultOf(lease)))
        assertTrue(registry.expired().none { it.submissionId == submission })
    }

    // --- 픽스처 ---

    protected fun origin() = SubmissionQueued(
        submissionId = submission,
        correlationId = "corr-1",
        problemId = "two-sum",
        problemVersion = 1,
        language = Language.KOTLIN,
        source = "fun twoSum(nums: IntArray, target: Int) = intArrayOf(0, 1)",
        requestTrace = true,
    )

    protected fun resultOf(lease: Lease) = resultOf(lease.attempt, lease.token)

    protected fun resultOf(attempt: Int, token: FencingToken) = ExecutionResult(
        executionId = "exec-$attempt",
        submissionId = submission,
        attempt = attempt,
        fencingToken = token,
        terminalVerdict = null,
        compileLog = null,
        cases = listOf(TestCaseResult("s1", "sample", Verdict.ACCEPTED, Measurements.NONE)),
        resultDigest = "digest-1",
    )
}

class MemoryLeaseRegistryTest : LeaseRegistryContract() {
    override fun registry() = MemoryLeaseRegistry(
        clock = clock, leaseDuration = Duration.ofSeconds(30), dispatchTimeout = Duration.ofMinutes(5),
    )
}
