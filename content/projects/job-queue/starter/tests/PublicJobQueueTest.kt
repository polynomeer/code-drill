package tests

import codedrill.*
import queue.JobQueue

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicJobQueueTest {

    fun testSubmitPollAck() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        val id = queue.submit("payload", 0)
        assertEquals("job-1", id)
        val lease = queue.poll(0)
        assertEquals(id, lease?.jobId)
        assertEquals("payload", lease?.payload)
        queue.ack(id, lease!!.token, 1)
        assertEquals(0, queue.pending(1))
    }

    fun testEmptyQueueGivesNull() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        assertNull(queue.poll(0))
    }

    fun testNackPutsItBack() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        queue.submit("a", 0)
        val first = queue.poll(0)!!
        queue.nack(first.jobId, first.token, 1)
        assertEquals(1, queue.pending(1))
        val second = queue.poll(1)
        assertEquals("job-1", second?.jobId)
    }

    fun testUnknownJob() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        assertThrows<NoSuchElementException> { queue.ack("job-9", 1, 0) }
    }

    fun testBadLimits() {
        assertThrows<IllegalArgumentException> { JobQueue(maxAttempts = 0, visibilityMillis = 100) }
    }
}
