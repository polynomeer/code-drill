package dev.codedrill.controlplane.integrity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FingerprintTest {

    private val original = """
        fun twoSum(nums: IntArray, target: Int): IntArray {
            val seen = HashMap<Int, Int>()
            for (i in nums.indices) {
                val j = seen[target - nums[i]]
                if (j != null) return intArrayOf(j, i)
                seen.putIfAbsent(nums[i], i)
            }
            error("no answer")
        }
    """.trimIndent()

    @Test
    fun `이름을 바꾸고 주석을 붙여도 같은 지문이다`() {
        val renamed = """
            // 해시맵으로 푼다
            fun twoSum(arr: IntArray, goal: Int): IntArray {
                val table = HashMap<Int, Int>() /* 값 → 인덱스 */
                for (k in arr.indices) {
                    val found = table[goal - arr[k]]
                    if (found != null) return intArrayOf(found, k)
                    table.putIfAbsent(arr[k], k)
                }
                error("없다")
            }
        """.trimIndent()
        assertEquals(1.0, Fingerprint.similarity(Fingerprint.of(original).hashes, Fingerprint.of(renamed).hashes))
    }

    @Test
    fun `다른 접근은 지문이 다르다`() {
        val quadratic = """
            fun twoSum(nums: IntArray, target: Int): IntArray {
                for (i in nums.indices) for (j in i + 1 until nums.size) {
                    if (nums[i] + nums[j] == target) return intArrayOf(i, j)
                }
                error("no answer")
            }
        """.trimIndent()
        val score = Fingerprint.similarity(Fingerprint.of(original).hashes, Fingerprint.of(quadratic).hashes)
        assertTrue(score < IntegrityService.THRESHOLD, "다른 접근이 문턱을 넘었다: $score")
    }

    @Test
    fun `줄 하나를 더해도 지문의 대부분이 남는다`() {
        // 60 토큰짜리 소스에 줄 하나는 10% 다. 문턱(0.8)은 못 넘어도 대부분은 남아야 한다 —
        // 창 단위로 뽑은 지문은 삽입 자리 근처만 바뀌고 나머지는 밀리지 않는다.
        val extended = original.replace("val seen = HashMap<Int, Int>()", "val seen = HashMap<Int, Int>()\n    var steps = 0")
        val score = Fingerprint.similarity(Fingerprint.of(original).hashes, Fingerprint.of(extended).hashes)
        assertTrue(score >= 0.7, "줄 하나에 지문이 무너졌다: $score")
    }

    @Test
    fun `빈 소스끼리는 같은 것이 아니다`() {
        assertEquals(0.0, Fingerprint.similarity(emptySet(), emptySet()))
    }
}
