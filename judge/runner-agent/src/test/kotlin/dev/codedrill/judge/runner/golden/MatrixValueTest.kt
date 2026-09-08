package dev.codedrill.judge.runner.golden

import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.FencingToken
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.judge.runner.execution.ExecutionEngine
import dev.codedrill.judge.runner.execution.adapter.JavaAdapter
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import dev.codedrill.judge.runner.execution.adapter.PythonAdapter
import dev.codedrill.judge.runner.execution.adapter.SandboxProtocol
import dev.codedrill.judge.runner.execution.sandbox.ProcessSandbox
import dev.codedrill.platform.problempackage.ProblemPackage
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import dev.codedrill.platform.problempackage.ValueType
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 격자 값 타입의 왕복 (기술 설계서 §5.3 prepare/check, §14.2 코퍼스).
 *
 * 콘텐츠 검증(§6.3)은 **코틀린 참조 구현만** 돌린다. 자바와 파이썬 하네스가 격자를
 * 다르게 풀거나 다르게 싣더라도 문제는 전부 통과하고, 그 어긋남은 사용자가 그 언어로
 * 처음 제출할 때 드러난다. [StringValueTest] 와 같은 이유로 여기서 세 언어를 함께 돌린다.
 *
 * | 문제 | 입력 → 출력 |
 * |---|---|
 * | island-perimeter | INT_MATRIX → INT |
 * | spiral-order | INT_MATRIX → INT_ARRAY |
 * | rotate-grid | INT_MATRIX → INT_MATRIX |
 * | spiral-fill | INT → INT_MATRIX |
 *
 * **직사각형과 빈 격자가 핵심이다.** 격자는 `<행>,<열>,<행 우선 원소>` 로 실린다. 크기를
 * 앞에 두지 않으면 `[]` 와 `[[], [], []]` 이 똑같이 빈 원소 목록이 되고, 행과 열을
 * 맞바꾼 구현이 정사각형에서는 통과한다. rotate-grid 의 2×3 케이스와 각 문제의
 * `no-columns` 케이스가 그 자리를 지키므로, 전체 케이스 통과가 곧 증명이다.
 */
class MatrixValueTest {

    private val engine = ExecutionEngine(
        adapters = listOf(KotlinAdapter(), JavaAdapter(), PythonAdapter())
            .associateBy { it.language },
        sandboxes = { ProcessSandbox() },
    )

    @Test
    fun `세 언어가 격자 입력과 출력을 똑같이 다룬다`() {
        for ((problemId, sources) in SOLUTIONS) {
            val pkg = ProblemPackageLoader(CONTENT).load(problemId)

            for ((language, source) in sources) {
                val result = engine.execute(request(pkg, language, source))

                assertEquals(
                    null, result.terminalVerdict,
                    "$problemId/$language 가 시작도 못 했다: ${result.compileLog}",
                )
                val failed = result.cases.filter { it.verdict != Verdict.ACCEPTED }
                assertTrue(
                    failed.isEmpty(),
                    "$problemId/$language 실패: " +
                        failed.joinToString { "${it.groupId}/${it.caseId}=${it.verdict}" },
                )
            }
        }
    }

    @Test
    fun `같은 격자 답은 언어와 무관하게 같은 digest 를 낸다`() {
        // 격자를 돌려주는 문제로 본다. digest 가 갈리면 같은 제출이 언어에 따라 다른
        // 결과로 기록된다.
        val pkg = ProblemPackageLoader(CONTENT).load("rotate-grid")
        val digests = SOLUTIONS.getValue("rotate-grid").map { (language, source) ->
            engine.execute(request(pkg, language, source)).resultDigest
        }

        assertEquals(1, digests.distinct().size, "언어별 digest 가 갈렸다: $digests")
    }

    @Test
    fun `크기를 앞에 실어 빈 격자와 빈 행들을 가른다`() {
        // 케이스로는 이 둘을 나란히 세울 수 없다 — 한쪽이 틀려도 다른 쪽이 통과하면
        // 아무것도 실패하지 않는다. 형식이 둘을 구분한다는 사실 자체를 여기서 못박는다.
        assertEquals("0,0", SandboxProtocol.encode(ValueType.INT_MATRIX, emptyList<List<Int>>()))
        assertEquals(
            "3,0",
            SandboxProtocol.encode(ValueType.INT_MATRIX, listOf(listOf<Int>(), listOf(), listOf())),
        )
        assertEquals(
            "2,3,1,2,3,4,5,6",
            SandboxProtocol.encode(ValueType.INT_MATRIX, listOf(listOf(1, 2, 3), listOf(4, 5, 6))),
        )
    }

    @Test
    fun `들쭉날쭉한 기대값은 실을 수 없다`() {
        // 행 길이가 다른 기대값은 콘텐츠의 실수다. 조용히 실으면 그 문제는 어떤 풀이도
        // 통과할 수 없는 채로 공개된다.
        assertFailsWith<IllegalStateException> {
            SandboxProtocol.encode(ValueType.INT_MATRIX, listOf(listOf(1, 2), listOf(3)))
        }
    }

    private fun request(pkg: ProblemPackage, language: Language, source: String) = ExecutionRequest(
        executionId = "exec-matrix",
        submissionId = "sub-matrix",
        attempt = 1,
        fencingToken = FencingToken(1),
        correlationId = "corr-matrix",
        problemVersionId = pkg.problemVersionId,
        packageDigest = pkg.packageDigest,
        language = language,
        source = source,
        signature = pkg.manifest.signature,
        limits = pkg.manifest.limits,
        // 성능 그룹은 뺀다. 여기서 확인하는 것은 형식이지 속도가 아니고, 300×300 격자를
        // 세 언어로 두 번씩 돌리면 이 테스트가 빌드에서 가장 느린 것이 된다.
        groups = pkg.groups.filterNot { it.policy.id == "performance" }
            .map { RequestedGroup(it.policy, it.cases) },
    )

    private companion object {
        val CONTENT: Path = Path.of("../../content/problems")

        /**
         * 문제별 세 언어 풀이.
         *
         * 코틀린도 패키지의 참조 구현을 그대로 쓰지 않고 여기 다시 적는다. 이 테스트가
         * 지키는 것은 정답이 아니라 **세 언어가 같은 형식으로 격자를 주고받는다**는
         * 사실이다.
         */
        val SOLUTIONS: Map<String, Map<Language, String>> = mapOf(
            // INT_MATRIX → INT
            "island-perimeter" to mapOf(
                Language.KOTLIN to """
                    fun perimeter(grid: Array<IntArray>): Int {
                        val rows = grid.size
                        val cols = if (rows == 0) 0 else grid[0].size
                        var total = 0
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                if (grid[r][c] != 1) continue
                                total += 4
                                if (r > 0 && grid[r - 1][c] == 1) total -= 1
                                if (r + 1 < rows && grid[r + 1][c] == 1) total -= 1
                                if (c > 0 && grid[r][c - 1] == 1) total -= 1
                                if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
                            }
                        }
                        return total
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int perimeter(int[][] grid) {
                            int rows = grid.length;
                            int cols = rows == 0 ? 0 : grid[0].length;
                            int total = 0;
                            for (int r = 0; r < rows; r++) {
                                for (int c = 0; c < cols; c++) {
                                    if (grid[r][c] != 1) continue;
                                    total += 4;
                                    if (r > 0 && grid[r - 1][c] == 1) total -= 1;
                                    if (r + 1 < rows && grid[r + 1][c] == 1) total -= 1;
                                    if (c > 0 && grid[r][c - 1] == 1) total -= 1;
                                    if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1;
                                }
                            }
                            return total;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def perimeter(grid):
                        rows = len(grid)
                        cols = len(grid[0]) if rows else 0
                        total = 0
                        for r in range(rows):
                            for c in range(cols):
                                if grid[r][c] != 1:
                                    continue
                                total += 4
                                if r > 0 and grid[r - 1][c] == 1:
                                    total -= 1
                                if r + 1 < rows and grid[r + 1][c] == 1:
                                    total -= 1
                                if c > 0 and grid[r][c - 1] == 1:
                                    total -= 1
                                if c + 1 < cols and grid[r][c + 1] == 1:
                                    total -= 1
                        return total
                """.trimIndent(),
            ),

            // INT_MATRIX → INT_ARRAY
            "spiral-order" to mapOf(
                Language.KOTLIN to """
                    fun spiralOrder(grid: Array<IntArray>): IntArray {
                        val rows = grid.size
                        val cols = if (rows == 0) 0 else grid[0].size
                        val out = IntArray(rows * cols)
                        val seen = Array(rows) { BooleanArray(cols) }
                        var r = 0
                        var c = 0
                        var dr = 0
                        var dc = 1
                        for (i in out.indices) {
                            out[i] = grid[r][c]
                            seen[r][c] = true
                            var nr = r + dr
                            var nc = c + dc
                            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr][nc]) {
                                val t = dr
                                dr = dc
                                dc = -t
                                nr = r + dr
                                nc = c + dc
                            }
                            r = nr
                            c = nc
                        }
                        return out
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int[] spiralOrder(int[][] grid) {
                            int rows = grid.length;
                            int cols = rows == 0 ? 0 : grid[0].length;
                            int[] out = new int[rows * cols];
                            boolean[][] seen = new boolean[Math.max(rows, 1)][Math.max(cols, 1)];
                            int r = 0, c = 0, dr = 0, dc = 1;
                            for (int i = 0; i < out.length; i++) {
                                out[i] = grid[r][c];
                                seen[r][c] = true;
                                int nr = r + dr, nc = c + dc;
                                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || seen[nr][nc]) {
                                    int t = dr;
                                    dr = dc;
                                    dc = -t;
                                    nr = r + dr;
                                    nc = c + dc;
                                }
                                r = nr;
                                c = nc;
                            }
                            return out;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def spiralOrder(grid):
                        rows = len(grid)
                        cols = len(grid[0]) if rows else 0
                        seen = [[False] * cols for _ in range(rows)]
                        out = []
                        r = c = 0
                        dr, dc = 0, 1
                        for _ in range(rows * cols):
                            out.append(grid[r][c])
                            seen[r][c] = True
                            nr, nc = r + dr, c + dc
                            if not (0 <= nr < rows and 0 <= nc < cols and not seen[nr][nc]):
                                dr, dc = dc, -dr
                                nr, nc = r + dr, c + dc
                            r, c = nr, nc
                        return out
                """.trimIndent(),
            ),

            // INT_MATRIX → INT_MATRIX
            "rotate-grid" to mapOf(
                Language.KOTLIN to """
                    fun rotate(grid: Array<IntArray>): Array<IntArray> {
                        val rows = grid.size
                        val cols = if (rows == 0) 0 else grid[0].size
                        return Array(cols) { c -> IntArray(rows) { r -> grid[rows - 1 - r][c] } }
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int[][] rotate(int[][] grid) {
                            int rows = grid.length;
                            int cols = rows == 0 ? 0 : grid[0].length;
                            int[][] out = new int[cols][rows];
                            for (int c = 0; c < cols; c++) {
                                for (int r = 0; r < rows; r++) out[c][r] = grid[rows - 1 - r][c];
                            }
                            return out;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def rotate(grid):
                        return [list(row) for row in zip(*grid[::-1])]
                """.trimIndent(),
            ),

            // INT → INT_MATRIX
            "spiral-fill" to mapOf(
                Language.KOTLIN to """
                    fun spiralFill(n: Int): Array<IntArray> {
                        val out = Array(n) { IntArray(n) }
                        var r = 0
                        var c = 0
                        var dr = 0
                        var dc = 1
                        for (value in 1..n * n) {
                            out[r][c] = value
                            var nr = r + dr
                            var nc = c + dc
                            if (nr !in 0 until n || nc !in 0 until n || out[nr][nc] != 0) {
                                val t = dr
                                dr = dc
                                dc = -t
                                nr = r + dr
                                nc = c + dc
                            }
                            r = nr
                            c = nc
                        }
                        return out
                    }
                """.trimIndent(),

                Language.JAVA to """
                    class Solution {
                        public int[][] spiralFill(int n) {
                            int[][] out = new int[n][n];
                            int r = 0, c = 0, dr = 0, dc = 1;
                            for (int value = 1; value <= n * n; value++) {
                                out[r][c] = value;
                                int nr = r + dr, nc = c + dc;
                                if (nr < 0 || nr >= n || nc < 0 || nc >= n || out[nr][nc] != 0) {
                                    int t = dr;
                                    dr = dc;
                                    dc = -t;
                                    nr = r + dr;
                                    nc = c + dc;
                                }
                                r = nr;
                                c = nc;
                            }
                            return out;
                        }
                    }
                """.trimIndent(),

                Language.PYTHON to """
                    def spiralFill(n):
                        grid = [[0] * n for _ in range(n)]
                        r = c = 0
                        dr, dc = 0, 1
                        for value in range(1, n * n + 1):
                            grid[r][c] = value
                            nr, nc = r + dr, c + dc
                            if not (0 <= nr < n and 0 <= nc < n and grid[nr][nc] == 0):
                                dr, dc = dc, -dr
                                nr, nc = r + dr, c + dc
                            r, c = nr, nc
                        return grid
                """.trimIndent(),
            ),
        )
    }
}
