package queue

/**
 * 재시도가 있는 작업 큐 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 *
 * 시간은 인자로만 들어온다. 안에서 시계를 읽지 않는다.
 */
class JobQueue(private val maxAttempts: Int, private val visibilityMillis: Long) {

    init {
        TODO("한도를 검사한다")
    }

    fun submit(payload: String, now: Long): String {
        TODO()
    }

    fun poll(now: Long): Lease? {
        TODO()
    }

    fun ack(jobId: String, token: Long, now: Long) {
        TODO()
    }

    fun nack(jobId: String, token: Long, now: Long) {
        TODO()
    }

    fun attempts(jobId: String): Int {
        TODO()
    }

    fun deadLetters(): List<String> {
        TODO()
    }

    fun pending(now: Long): Int {
        TODO()
    }
}
