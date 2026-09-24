package queue

/** 참조 구현 (§6.3). 상태는 셋 — 대기·임대·끝남. 시간은 인자로만 들어온다. */
class JobQueue(private val maxAttempts: Int, private val visibilityMillis: Long) {

    private enum class State { READY, LEASED, DONE, DEAD }

    private class Job(
        val id: String,
        val payload: String,
        var state: State,
        var attempts: Int = 0,
        var token: Long = 0,
        var leaseExpiresAt: Long = 0,
        var readyAt: Long = 0,
    )

    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
        require(visibilityMillis >= 0) { "visibilityMillis must not be negative" }
    }

    private val jobs = LinkedHashMap<String, Job>()
    private var submitted = 0L
    private var issuedTokens = 0L

    fun submit(payload: String, now: Long): String {
        val id = "job-${++submitted}"
        jobs[id] = Job(id, payload, State.READY, readyAt = now)
        return id
    }

    fun poll(now: Long): Lease? {
        reclaim(now)
        val job = jobs.values.firstOrNull { it.state == State.READY } ?: return null
        job.state = State.LEASED
        job.attempts += 1
        job.token = ++issuedTokens
        job.leaseExpiresAt = now + visibilityMillis
        return Lease(job.id, job.payload, job.token)
    }

    fun ack(jobId: String, token: Long, now: Long) {
        val job = leased(jobId, token, now)
        job.state = State.DONE
    }

    fun nack(jobId: String, token: Long, now: Long) {
        val job = leased(jobId, token, now)
        fail(job, now)
    }

    fun attempts(jobId: String): Int = job(jobId).attempts

    fun deadLetters(): List<String> = jobs.values.filter { it.state == State.DEAD }.map { it.id }

    fun pending(now: Long): Int {
        reclaim(now)
        return jobs.values.count { it.state == State.READY || it.state == State.LEASED }
    }

    private fun reclaim(now: Long) {
        for (job in jobs.values) {
            if (job.state == State.LEASED && job.leaseExpiresAt <= now) fail(job, now)
        }
    }

    private fun fail(job: Job, now: Long) {
        job.token = 0
        if (job.attempts >= maxAttempts) {
            job.state = State.DEAD
        } else {
            job.state = State.READY
            job.readyAt = now
        }
    }

    private fun job(jobId: String): Job =
        jobs[jobId] ?: throw NoSuchElementException("unknown job: $jobId")

    private fun leased(jobId: String, token: Long, now: Long): Job {
        reclaim(now)
        val job = job(jobId)
        check(job.state == State.LEASED) { "job is not leased: $jobId (${job.state})" }
        check(job.token == token) { "stale token for $jobId" }
        return job
    }
}
