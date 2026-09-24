package tests

import codedrill.*
import queue.JobQueue

/** 숨은 테스트. 사용자에게 나가지 않는다 (§8.3). */
class HiddenJobQueueTest {

    fun testAttemptsCountedOnPoll() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        assertEquals(0, queue.attempts(id))
        val lease = queue.poll(0)!!
        assertEquals(1, queue.attempts(id))
        queue.nack(id, lease.token, 1)
        assertEquals(1, queue.attempts(id))
        queue.poll(1)
        assertEquals(2, queue.attempts(id))
    }

    fun testDeadAfterMaxAttempts() {
        val queue = JobQueue(maxAttempts = 2, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        val first = queue.poll(0)!!
        queue.nack(id, first.token, 1)
        assertEquals(listOf<String>(), queue.deadLetters())
        val second = queue.poll(1)!!
        queue.nack(id, second.token, 2)
        assertEquals(listOf("job-1"), queue.deadLetters())
        assertEquals(0, queue.pending(2))
        assertNull(queue.poll(3))
    }

    fun testExpiredLeaseIsReclaimed() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        queue.poll(0)
        assertNull(queue.poll(99))
        val again = queue.poll(100)
        assertEquals(id, again?.jobId)
        assertEquals(2, queue.attempts(id))
    }

    fun testExpiryBoundaryIsInclusive() {
        val queue = JobQueue(maxAttempts = 5, visibilityMillis = 10)
        queue.submit("a", 0)
        val lease = queue.poll(5)!!
        assertNull(queue.poll(14))
        assertEquals(lease.jobId, queue.poll(15)?.jobId)
    }

    fun testStaleTokenIsRejected() {
        val queue = JobQueue(maxAttempts = 5, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        val first = queue.poll(0)!!
        val second = queue.poll(100)!!
        assertTrue(first.token != second.token)
        assertThrows<IllegalStateException> { queue.ack(id, first.token, 101) }
        queue.ack(id, second.token, 101)
        assertEquals(0, queue.pending(101))
    }

    fun testAckTwiceIsRejected() {
        val queue = JobQueue(maxAttempts = 5, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        val lease = queue.poll(0)!!
        queue.ack(id, lease.token, 1)
        assertThrows<IllegalStateException> { queue.ack(id, lease.token, 2) }
    }

    fun testNackOnWaitingJobIsRejected() {
        val queue = JobQueue(maxAttempts = 5, visibilityMillis = 100)
        val id = queue.submit("a", 0)
        assertThrows<IllegalStateException> { queue.nack(id, 1, 0) }
    }

    fun testSubmissionOrderIsKept() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        queue.submit("a", 0)
        queue.submit("b", 0)
        val first = queue.poll(0)!!
        assertEquals("job-1", first.jobId)
        queue.nack(first.jobId, first.token, 1)
        // 실패해 돌아온 job-1 이 아직 한 번도 주지 않은 job-2 보다 앞이다.
        assertEquals("job-1", queue.poll(1)?.jobId)
        assertEquals("job-2", queue.poll(1)?.jobId)
    }

    fun testExpiryCanKillTheJob() {
        val queue = JobQueue(maxAttempts = 1, visibilityMillis = 10)
        val id = queue.submit("a", 0)
        queue.poll(0)
        assertEquals(0, queue.pending(10))
        assertEquals(listOf(id), queue.deadLetters())
    }

    fun testZeroVisibilityExpiresImmediately() {
        val queue = JobQueue(maxAttempts = 2, visibilityMillis = 0)
        val id = queue.submit("a", 0)
        val lease = queue.poll(0)!!
        // 시한이 0 이라 같은 시각의 다음 호출에서 이미 지났다 — 보고는 거절되고 작업은 대기로 돌아온다.
        assertThrows<IllegalStateException> { queue.ack(id, lease.token, 0) }
        assertEquals(listOf<String>(), queue.deadLetters())
        assertEquals(1, queue.pending(0))
        // 한 번 더 주면 한도에 닿는다. 그 임대도 곧바로 지났고, 다음 호출이 회수하며 죽인다.
        queue.poll(0)
        assertEquals(2, queue.attempts(id))
        assertEquals(0, queue.pending(0))
        assertEquals(listOf(id), queue.deadLetters())
    }

    fun testDeadLettersFollowSubmissionOrder() {
        val queue = JobQueue(maxAttempts = 1, visibilityMillis = 100)
        queue.submit("a", 0)
        queue.submit("b", 0)
        val second = queue.poll(0)!!
        val first = queue.poll(0)!!
        assertEquals("job-1", second.jobId)
        assertEquals("job-2", first.jobId)
        queue.nack("job-2", first.token, 1)
        queue.nack("job-1", second.token, 2)
        assertEquals(listOf("job-1", "job-2"), queue.deadLetters())
    }

    fun testPendingCountsLeasedAndWaiting() {
        val queue = JobQueue(maxAttempts = 3, visibilityMillis = 100)
        queue.submit("a", 0)
        queue.submit("b", 0)
        queue.submit("c", 0)
        val lease = queue.poll(0)!!
        assertEquals(3, queue.pending(0))
        queue.ack(lease.jobId, lease.token, 1)
        assertEquals(2, queue.pending(1))
    }

    fun testTokensNeverRepeat() {
        val queue = JobQueue(maxAttempts = 5, visibilityMillis = 10)
        queue.submit("a", 0)
        queue.submit("b", 0)
        val seen = HashSet<Long>()
        var now = 0L
        repeat(6) {
            val lease = queue.poll(now)
            if (lease != null) assertTrue(seen.add(lease.token))
            now += 10
        }
        assertTrue(seen.size >= 4)
    }
}
