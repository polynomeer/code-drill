package dev.codedrill.controlplane.learning

import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 처방 조정의 두 행동 (UI §9.4 "시작·교체·미루기").
 *
 * 교체는 오늘 하루, 미루기는 며칠이다. 둘 다 "그날 밀어낸 문제"라는 같은 사실로 적히므로,
 * 다른 것은 몇 날짜에 적었는가뿐이다 — 그것을 본다.
 */
class LearningServiceTest {

    private val zone = ZoneId.of("Asia/Seoul")

    /** 23:30 KST — UTC 로는 같은 날 14:30. 날짜는 서울 기준이어야 한다. */
    private val now = Instant.parse("2026-10-03T14:30:00Z")

    @Test
    fun `교체는 오늘 하루만 밀어낸다`() {
        val repository = RecordingRepository()
        service(repository).skip("u1", "two-sum", now)
        assertEquals(listOf(LocalDate.of(2026, 10, 3)), repository.skips.map { it.second })
    }

    @Test
    fun `미루기는 오늘부터 사흘을 밀어낸다`() {
        val repository = RecordingRepository()
        service(repository).defer("u1", "two-sum", now)
        assertEquals(
            listOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5)),
            repository.skips.map { it.second },
        )
        assertEquals(setOf("two-sum"), repository.skips.map { it.first }.toSet())
    }

    private fun service(repository: LearningRepository) = LearningService(
        repository = repository,
        packages = ProblemPackageLoader(Files.createTempDirectory("problems")),
        zone = zone,
    )

    private class RecordingRepository : LearningRepository(JdbcTemplate()) {
        val skips = mutableListOf<Pair<String, LocalDate>>()

        override fun skip(userId: String, problemId: String, on: LocalDate): Int {
            skips += problemId to on
            return 1
        }

        override fun skipped(userId: String, on: LocalDate): Set<String> =
            skips.filter { it.second == on }.map { it.first }.toSet()
    }
}
