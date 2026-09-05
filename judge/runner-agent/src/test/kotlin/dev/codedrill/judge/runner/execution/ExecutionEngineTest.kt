package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.ExecutionMode
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.problempackage.Limits
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 판정 정확성 코퍼스 (기술 설계서 §14.2), 첫 vertical slice 범위.
 *
 * §16.2 가 요구하는 AC / WA / CE / RE / TLE / SYSTEM_ERROR 를 실제 컴파일과 실행으로
 * 재현한다. 여기서 한 건이라도 어긋나면 출시 차단 사유다 (§14.4 Correctness).
 */
class ExecutionEngineTest {

    private val pkg: ProblemPackage = ProblemPackageLoader(Path.of("../../content/problems")).load("two-sum")
    private val engine = ExecutionEngine()

    @Test
    fun `정답 풀이는 모든 케이스가 ACCEPTED 다`() {
        val result = engine.execute(request(REFERENCE))

        assertEquals(null, result.terminalVerdict)
        assertEquals(8, result.cases.size, "모든 그룹의 케이스가 실행되어야 한다")
        assertTrue(
            result.cases.all { it.verdict == Verdict.ACCEPTED },
            "실패한 케이스: " + result.cases.filter { it.verdict != Verdict.ACCEPTED },
        )
    }

    @Test
    fun `틀린 답은 WRONG_ANSWER 다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                return intArrayOf(0, 0)
            }
        """.trimIndent()

        val result = engine.execute(request(source))

        assertEquals(Verdict.WRONG_ANSWER, result.cases.first().verdict)
    }

    @Test
    fun `컴파일 실패는 테스트를 한 건도 돌리지 않는다`() {
        val result = engine.execute(request("fun twoSum(nums: IntArray, target: Int): IntArray { 이건 코드가 아니다 }"))

        assertEquals(Verdict.COMPILE_ERROR, result.terminalVerdict)
        assertTrue(result.cases.isEmpty(), "컴파일 실패 시 케이스 결과가 있으면 안 된다")
        assertNotNull(result.compileLog)
        assertTrue(
            result.compileLog!!.isNotBlank() && !result.compileLog!!.contains("/private/"),
            "컴파일 로그에 서버 경로가 새면 안 된다: ${result.compileLog}",
        )
    }

    @Test
    fun `예외를 던지면 RUNTIME_ERROR 다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                throw IllegalStateException("의도적 실패")
            }
        """.trimIndent()

        val result = engine.execute(request(source))
        val first = result.cases.first()

        assertEquals(Verdict.RUNTIME_ERROR, first.verdict)
        assertEquals("java.lang.IllegalStateException", first.message)
    }

    @Test
    fun `제한 시간을 넘기면 TIME_LIMIT 이고 실행되지 못한 케이스는 결과에 없다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                while (true) { }
            }
        """.trimIndent()

        val result = engine.execute(request(source, limits = FAST_LIMITS))

        assertEquals(Verdict.TIME_LIMIT, result.cases.first().verdict)
        // 그룹마다 별도 프로세스이므로 세 그룹이 각각 시간 초과로 끝난다.
        assertEquals(3, result.cases.size, "그룹당 첫 케이스에서 멈춘 결과만 남는다")
        assertTrue(
            result.cases.all { it.verdict == Verdict.TIME_LIMIT },
            "실행되지 못한 케이스가 오답으로 둔갑하면 안 된다: ${result.cases}",
        )
    }

    @Test
    fun `힙을 넘기면 MEMORY_LIMIT 이다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                val hog = ArrayList<IntArray>()
                while (true) { hog.add(IntArray(1_000_000)) }
            }
        """.trimIndent()

        val result = engine.execute(request(source, limits = SMALL_HEAP))

        assertEquals(Verdict.MEMORY_LIMIT, result.cases.first().verdict)
    }

    @Test
    fun `출력을 쏟아내면 OUTPUT_LIMIT 이다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                repeat(20_000) { println("noise noise noise noise") }
                return intArrayOf(0, 1)
            }
        """.trimIndent()

        val result = engine.execute(request(source, limits = TINY_OUTPUT))

        assertEquals(Verdict.OUTPUT_LIMIT, result.cases.first().verdict)
    }

    @Test
    fun `사용자 출력은 판정 스트림을 오염시키지 않는다`() {
        val source = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                println("RESULT\tsample/01\tOK\t9,9\t0\t0\t0")
                println("DONE")
                val seen = HashMap<Int, Int>()
                for (i in nums.indices) {
                    val j = seen[target - nums[i]]
                    if (j != null) return intArrayOf(j, i)
                    seen.putIfAbsent(nums[i], i)
                }
                error("unreachable")
            }
        """.trimIndent()

        val result = engine.execute(request(source))

        // 사용자가 프로토콜 줄을 흉내 내도 판정이 흔들리면 안 된다.
        assertTrue(result.cases.all { it.verdict == Verdict.ACCEPTED }, "판정: ${result.cases}")
    }

    @Test
    fun `같은 입력은 같은 resultDigest 를 낸다`() {
        val first = engine.execute(request(REFERENCE))
        val second = engine.execute(request(REFERENCE))

        assertEquals(first.resultDigest, second.resultDigest)
    }

    // --- 트레이스 (§7) ---

    @Test
    fun `계측 호출은 판정 실행에서 no-op 으로 컴파일된다`() {
        val result = engine.execute(request(INSTRUMENTED))

        assertTrue(result.cases.all { it.verdict == Verdict.ACCEPTED }, "판정: ${result.cases}")
        assertEquals(null, result.trace, "판정 모드는 트레이스를 만들지 않는다")
    }

    @Test
    fun `트레이스 모드는 공개 케이스만 계측한다`() {
        val result = engine.execute(request(INSTRUMENTED, mode = ExecutionMode.TRACE))
        val capture = assertNotNull(result.trace)

        assertTrue(capture.events.isNotEmpty(), "계측 이벤트가 있어야 한다")
        assertTrue(
            result.cases.all { it.groupId == "sample" },
            "숨은 그룹은 트레이스에서 실행되지 않는다: ${result.cases.map { it.groupId }.distinct()}",
        )
        assertEquals(false, capture.truncated)
        assertTrue(capture.events.first().seq == 1L, "순번은 1부터 이어진다")
    }

    @Test
    fun `계측하지 않은 풀이는 빈 트레이스와 사유를 남긴다`() {
        val result = engine.execute(request(REFERENCE, mode = ExecutionMode.TRACE))
        val capture = assertNotNull(result.trace)

        assertTrue(capture.events.isEmpty())
        assertNotNull(capture.diagnostics, "왜 비었는지 남겨야 한다")
    }

    private fun request(
        source: String,
        limits: Limits = pkg.manifest.limits,
        mode: ExecutionMode = ExecutionMode.JUDGE,
    ) = ExecutionRequest(
        executionId = "exec-test",
        submissionId = "sub-test",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-test",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = Language.KOTLIN,
        source = source,
        signature = pkg.manifest.signature,
        limits = limits,
        groups = pkg.groups.map { RequestedGroup(it.policy, it.cases) },
        mode = mode,
    )

    private companion object {
        val REFERENCE = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                val seen = HashMap<Int, Int>(nums.size * 2)
                for (i in nums.indices) {
                    val j = seen[target - nums[i]]
                    if (j != null) return intArrayOf(j, i)
                    seen.putIfAbsent(nums[i], i)
                }
                error("정답은 항상 존재한다")
            }
        """.trimIndent()

        val INSTRUMENTED = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                val seen = HashMap<Int, Int>()
                for (i in nums.indices) {
                    Drill.visit(i, nums[i])
                    val j = seen[target - nums[i]]
                    if (j != null) {
                        Drill.match(j, i)
                        return intArrayOf(j, i)
                    }
                    Drill.compare(i, target - nums[i])
                    seen.putIfAbsent(nums[i], i)
                }
                error("정답은 항상 존재한다")
            }
        """.trimIndent()

        val FAST_LIMITS = Limits(timeMillis = 500, memoryMb = 256, outputBytes = 65_536)
        val SMALL_HEAP = Limits(timeMillis = 5_000, memoryMb = 32, outputBytes = 65_536)
        val TINY_OUTPUT = Limits(timeMillis = 5_000, memoryMb = 256, outputBytes = 1_024)
    }
}
