package dev.codedrill.controlplane.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 계측 원칙 (디자인 설계서 §16.2) — 소스·입력·출력 전문이 들어올 자리가 없다. */
class EventSchemaTest {

    @Test
    fun `표에 있는 이벤트와 속성만 받는다`() {
        assertEquals(
            mapOf("problem" to "two-sum", "language" to "PYTHON"),
            EventSchema.clean("submission_created", mapOf("problem" to "two-sum", "language" to "PYTHON", "userId" to "u1")),
        )
        assertNull(EventSchema.clean("keystroke", mapOf("key" to "a")))
    }

    @Test
    fun `코드나 긴 글은 식별자 칸에 들어오지 못한다`() {
        val cleaned = EventSchema.clean(
            "submission_created",
            mapOf("problem" to "def twoSum(nums):\n    return []", "language" to "PYTHON"),
        )
        assertEquals(mapOf("language" to "PYTHON"), cleaned)
        assertEquals(emptyMap(), EventSchema.clean("problem_open", mapOf("problemId" to "x".repeat(65))))
    }

    @Test
    fun `정해진 값과 수만 받는다`() {
        assertEquals(emptyMap(), EventSchema.clean("run_requested", mapOf("type" to "everything", "language" to "RUST")))
        assertEquals(mapOf("from" to 3.0, "to" to 8.0, "method" to "key"), EventSchema.clean("replay_seeked", mapOf("from" to 3, "to" to 8, "method" to "key")))
        assertEquals(emptyMap(), EventSchema.clean("verdict_viewed", mapOf("latency" to "fast")))
    }
}
