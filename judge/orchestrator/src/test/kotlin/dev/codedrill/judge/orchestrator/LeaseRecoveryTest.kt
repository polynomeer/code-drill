package dev.codedrill.judge.orchestrator

import dev.codedrill.judge.orchestrator.lease.AttemptRegistry
import dev.codedrill.judge.protocol.ExecutionHeartbeat
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.JudgeCompleted
import dev.codedrill.judge.protocol.JudgeProgressed
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.Measurements
import dev.codedrill.judge.protocol.SubmissionQueued
import dev.codedrill.judge.protocol.TestCaseResult
import dev.codedrill.judge.protocol.TraceReady
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 워커 유실에서의 회복 (기술 설계서 §4.3, §19.1 "워커 유실을 주입해 결과 수렴 확인").
 *
 * 워커가 죽으면 결과는 영영 오지 않는다. 임대 만료를 회수하는 경로가 없으면 제출은
 * LEASED 에 멈춘 채 남고, 사용자는 끝나지 않는 채점을 본다. 이 테스트가 그 경로와,
 * **회수가 중복 판정을 만들지 않는다**는 것을 함께 고정한다.
 */
class LeaseRecoveryTest {

    @Test
    fun `큐에서 차례를 기다리는 것은 워커 유실이 아니다`() {
        val world = World()
        world.queue()

        // 아무 워커도 집어 들지 않았다. Runner 가 다른 제출을 돌리는 동안의 정상 상태다.
        world.advance(Duration.ofMinutes(2))
        world.coordinator.reclaimExpiredLeases()

        assertEquals(
            1, world.gateway.requests.size,
            "큐 대기를 유실로 세면, 바쁠 때만 멀쩡한 제출이 두 번 돌고 SYSTEM_ERROR 가 된다",
        )
    }

    @Test
    fun `임대가 만료되면 실행을 다시 건다`() {
        val world = World()

        world.queue()
        assertEquals(1, world.gateway.requests.size)
        world.pickUp()

        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()

        assertEquals(2, world.gateway.requests.size, "만료된 임대는 다시 실행에 걸린다")
        val retry = world.gateway.requests.last()
        assertEquals(2, retry.attempt)
        assertTrue(
            retry.fencingToken > world.gateway.requests.first().fencingToken,
            "재실행은 토큰을 올려 이전 실행을 무효로 만든다",
        )
    }

    @Test
    fun `살아 돌아온 워커의 결과는 거절된다`() {
        val world = World()
        world.queue()
        world.pickUp()
        val lost = world.gateway.requests.first()

        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()

        // 죽은 줄 알았던 워커가 뒤늦게 결과를 보낸다.
        world.coordinator.onExecutionResult(world.resultOf(lost), lost.correlationId)

        assertTrue(world.gateway.completed.isEmpty(), "낮은 토큰의 결과는 판정이 되지 않는다")
    }

    @Test
    fun `재실행한 워커의 결과는 정상 판정이 된다`() {
        val world = World()
        world.queue()
        world.pickUp()

        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()
        val retry = world.gateway.requests.last()

        world.coordinator.onExecutionResult(world.resultOf(retry), retry.correlationId)

        assertEquals(1, world.gateway.completed.size)
        assertEquals(Verdict.ACCEPTED, world.gateway.completed.single().verdict)
    }

    @Test
    fun `심장 박동이 오는 동안에는 회수하지 않는다`() {
        val world = World()
        world.queue()
        val request = world.gateway.requests.single()

        // 실행이 임대보다 오래 걸린다. 워커는 살아서 붙들고 있다.
        repeat(4) {
            world.advance(Duration.ofSeconds(10))
            world.coordinator.onHeartbeat(world.heartbeatFor(request))
            world.coordinator.reclaimExpiredLeases()
        }

        assertEquals(
            1, world.gateway.requests.size,
            "밀린 실행을 워커 유실로 오해하면, 바쁠 때만 멀쩡한 제출이 SYSTEM_ERROR 가 된다",
        )
    }

    @Test
    fun `심장 박동이 끊기면 회수한다`() {
        val world = World()
        world.queue()
        val request = world.gateway.requests.single()

        world.advance(Duration.ofSeconds(20))
        world.coordinator.onHeartbeat(world.heartbeatFor(request))
        world.coordinator.reclaimExpiredLeases()
        assertEquals(1, world.gateway.requests.size, "박동이 있는 동안은 살아 있다")

        // 여기서 워커가 죽는다. 더 이상 박동이 오지 않는다.
        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()

        assertEquals(2, world.gateway.requests.size, "박동이 멈추면 유실이다")
    }

    @Test
    fun `무효가 된 워커의 심장 박동은 임대를 살리지 못한다`() {
        val world = World()
        world.queue()
        world.pickUp()
        val lost = world.gateway.requests.first()

        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()
        val retry = world.gateway.requests.last()
        world.pickUp()

        // 죽은 줄 알았던 워커가 살아나 계속 박동을 보낸다.
        world.advance(Duration.ofSeconds(31))
        world.coordinator.onHeartbeat(world.heartbeatFor(lost))
        world.coordinator.reclaimExpiredLeases()

        assertEquals(3, world.gateway.requests.size, "스테일 워커가 현재 임대를 붙들면 안 된다")
        assertTrue(world.gateway.requests.last().fencingToken > retry.fencingToken)
    }

    @Test
    fun `재시도 한계를 넘으면 SYSTEM_ERROR 로 끝낸다`() {
        val world = World(maxAttempts = 2)
        world.queue()

        // 매번 워커가 집어 들었다가 사라진다.
        repeat(3) {
            world.pickUp()
            world.advance(Duration.ofSeconds(31))
            world.coordinator.reclaimExpiredLeases()
        }

        val completed = world.gateway.completed.single()
        assertEquals(Verdict.SYSTEM_ERROR, completed.verdict, "무한 재시도 대신 드러나게 끝낸다 (§4.4)")
        assertEquals(0, completed.score)

        // 포기한 뒤 결과가 도착해도 이미 사용자에게 보인 판정을 덮지 않는다.
        val late = world.gateway.requests.last()
        world.coordinator.onExecutionResult(world.resultOf(late), late.correlationId)
        assertEquals(1, world.gateway.completed.size, "종료는 불변이다 (§4.2)")
    }

    @Test
    fun `결과가 온 실행은 회수 대상이 아니다`() {
        val world = World()
        world.queue()
        world.pickUp()
        val request = world.gateway.requests.single()
        world.coordinator.onExecutionResult(world.resultOf(request), request.correlationId)

        world.advance(Duration.ofSeconds(31))
        world.coordinator.reclaimExpiredLeases()

        assertEquals(1, world.gateway.requests.size, "끝난 실행을 다시 걸면 중복 판정이 된다")
    }

    // --- 픽스처 ---

    /** 시계를 손으로 돌릴 수 있는 오케스트레이터 한 벌. */
    private class World(maxAttempts: Int = 3) {
        var now: Instant = Instant.parse("2026-09-07T00:00:00Z")

        val gateway = RecordingGateway()

        private val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = now
        }

        val coordinator = JudgeCoordinator(
            packages = ProblemPackageLoader(Path.of(CONTENT_ROOT)),
            registry = AttemptRegistry(
                clock = clock,
                leaseDuration = Duration.ofSeconds(30),
                dispatchTimeout = Duration.ofMinutes(5),
            ),
            gateway = gateway,
            maxAttempts = maxAttempts,
        )

        fun advance(by: Duration) {
            now = now.plus(by)
        }

        fun queue() = coordinator.onSubmissionQueued(
            SubmissionQueued(
                submissionId = SUBMISSION,
                correlationId = "corr-1",
                problemId = PROBLEM,
                problemVersion = 1,
                language = Language.KOTLIN,
                source = "fun twoSum(nums: IntArray, target: Int) = intArrayOf(0, 1)",
                requestTrace = false,
            ),
        )

        /** 가장 최근에 걸린 실행을 워커가 집어 들었다고 알린다. */
        fun pickUp() = coordinator.onHeartbeat(heartbeatFor(gateway.requests.last()))

        fun heartbeatFor(request: ExecutionRequest) = ExecutionHeartbeat(
            submissionId = request.submissionId,
            executionId = request.executionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
        )

        /** 요청이 지목한 모든 케이스를 통과한 결과. */
        fun resultOf(request: ExecutionRequest) = dev.codedrill.judge.protocol.ExecutionResult(
            executionId = request.executionId,
            submissionId = request.submissionId,
            attempt = request.attempt,
            fencingToken = request.fencingToken,
            problemVersionId = request.problemVersionId,
            terminalVerdict = null,
            compileLog = null,
            cases = request.groups.flatMap { group ->
                group.cases.map {
                    TestCaseResult("${group.policy.id}/${it.id}", group.policy.id, Verdict.ACCEPTED, Measurements.NONE)
                }
            },
            resultDigest = "digest-${request.executionId}",
        )
    }

    private class RecordingGateway : JudgeGateway {
        val requests = mutableListOf<ExecutionRequest>()
        val completed = mutableListOf<JudgeCompleted>()

        override fun requestExecution(request: ExecutionRequest) {
            requests += request
        }

        override fun publishProgress(progress: JudgeProgressed) = Unit
        override fun publishCompleted(completed: JudgeCompleted) {
            this.completed += completed
        }

        override fun publishTraceReady(ready: TraceReady) = Unit
    }

    private companion object {
        const val SUBMISSION = "sub-recovery"
        const val PROBLEM = "two-sum"

        /** 테스트의 작업 디렉터리는 모듈 루트다. 문제 패키지는 저장소 루트 아래에 있다. */
        const val CONTENT_ROOT = "../../content/problems"
    }
}
