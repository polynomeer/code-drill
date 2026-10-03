package dev.codedrill.controlplane.problem

import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 문제 목록의 쪽 번호·정렬·번호 검색 (docs/ui-overhaul.md §6.1).
 *
 * 저장소의 실제 문제를 읽는다. 정답률은 가짜로 꽂는다 — 제출 도메인을 띄울 이유가 없다.
 */
class ProblemControllerTest {

    private val root = "../../content/problems"

    /** two-sum 은 표본 충분(정답률 20%), max-subarray 는 80%, 나머지는 표본 없음. */
    private val progress = object : ProblemProgress {
        override fun accuracy() = mapOf(
            "two-sum" to ProblemProgress.Accuracy(attempted = 10, solved = 2),
            "max-subarray" to ProblemProgress.Accuracy(attempted = 10, solved = 8),
        )

        override fun solvedBy(userId: String) = setOf("two-sum")
    }

    private val controller = ProblemController(
        packages = ProblemPackageLoader(Path.of(root)),
        published = { emptySet() },
        progress = progress,
        contentRoot = root,
        requirePublish = false,
    )

    private fun list(
        query: String? = null,
        sort: ProblemController.Sort? = null,
        order: ProblemController.Order? = null,
        page: Int? = null,
        limit: Int? = null,
        principal: Principal? = null,
    ) = controller.list(query, null, null, null, null, null, limit, sort, order, page, principal)

    @Test
    fun `기본은 커서 방식이고 쪽 번호를 싣지 않는다`() {
        val page = list(limit = 5)
        assertNotNull(page.nextCursor)
        assertNull(page.page)
        assertEquals(page.items.map { it.id }.sorted(), page.items.map { it.id }, "커서 방식은 id 순이다")
    }

    @Test
    fun `번호 순 쪽 번호 방식은 1000번부터 시작한다`() {
        val first = list(sort = ProblemController.Sort.NUMBER, limit = 50)
        assertEquals(1, first.page)
        assertEquals(1000, first.items.first().number)
        assertEquals("two-sum", first.items.first().id)
        assertEquals((first.total + 49) / 50, first.pageCount)

        val last = list(sort = ProblemController.Sort.NUMBER, limit = 50, page = 999)
        assertEquals(first.pageCount, last.page, "범위를 넘는 쪽은 마지막 쪽이다")
    }

    @Test
    fun `정답률 정렬은 표본이 없는 문제를 방향과 상관없이 맨 뒤로 보낸다`() {
        val asc = list(sort = ProblemController.Sort.ACCURACY, limit = 100).items
        assertEquals(listOf("two-sum", "max-subarray"), asc.take(2).map { it.id })
        assertTrue(asc.drop(2).all { it.solvedRate == null })

        val desc = list(sort = ProblemController.Sort.ACCURACY, order = ProblemController.Order.DESC, limit = 100).items
        assertEquals(listOf("max-subarray", "two-sum"), desc.take(2).map { it.id })
    }

    @Test
    fun `숫자로 검색하면 그 번호의 문제가 나온다`() {
        val found = list(query = "#1000", sort = ProblemController.Sort.NUMBER).items
        assertEquals(listOf("two-sum"), found.map { it.id })
        // 번호는 부분 일치가 아니다 — "10" 이 1000~1099 를 다 끌어오지 않는다
        assertTrue(list(query = "10", sort = ProblemController.Sort.NUMBER).items.none { it.number == 1000 })
    }

    @Test
    fun `아무 문제나는 로그인했으면 안 푼 문제에서 고른다`() {
        val me = Principal("u1", "시험")
        repeat(20) {
            val picked = assertNotNull(controller.random("two-sum", null, null, null, null, me).body)
            // 조건에 맞는 것이 이미 푼 two-sum 하나뿐이면 그것이라도 준다
            assertEquals("two-sum", picked.id)
        }
        repeat(20) {
            val picked = assertNotNull(controller.random(null, null, null, null, null, me).body)
            assertTrue(picked.id != "two-sum", "푼 문제로 데려가지 않는다")
        }
    }

    @Test
    fun `상세에 번호가 실린다`() {
        assertEquals(1000, controller.detail("two-sum").body?.number)
    }
}
