package dev.codedrill.controlplane.submission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmissionStatusTest {

    @Test
    fun `정상 경로는 CREATED 에서 COMPLETED 까지 이어진다`() {
        val path = listOf(
            SubmissionStatus.CREATED,
            SubmissionStatus.QUEUED,
            SubmissionStatus.LEASED,
            SubmissionStatus.COMPILING,
            SubmissionStatus.RUNNING,
            SubmissionStatus.AGGREGATING,
            SubmissionStatus.COMPLETED,
        )
        path.zipWithNext { from, to ->
            assertTrue(from.canTransitionTo(to), "$from → $to 는 허용되어야 한다")
        }
    }

    @Test
    fun `종료 상태에서는 어떤 전이도 나가지 않는다`() {
        listOf(
            SubmissionStatus.COMPLETED,
            SubmissionStatus.CANCELLED,
            SubmissionStatus.SYSTEM_ERROR,
        ).forEach { terminal ->
            assertTrue(terminal.terminal, "$terminal 은 종료 상태다")
            SubmissionStatus.entries.forEach { next ->
                assertFalse(terminal.canTransitionTo(next), "$terminal → $next 는 막혀야 한다")
            }
        }
    }

    @Test
    fun `lease 만료는 QUEUED 로 되돌아간다`() {
        assertTrue(SubmissionStatus.LEASED.canTransitionTo(SubmissionStatus.QUEUED))
    }

    @Test
    fun `건너뛰는 전이는 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            SubmissionStatus.QUEUED.transitionTo(SubmissionStatus.RUNNING)
        }
    }

    @Test
    fun `합법 전이는 다음 상태를 돌려준다`() {
        assertEquals(
            SubmissionStatus.QUEUED,
            SubmissionStatus.CREATED.transitionTo(SubmissionStatus.QUEUED),
        )
    }
}
