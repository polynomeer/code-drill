package queue

/** 워커가 받아 든 작업 한 건. 값 객체라 바뀌지 않는다. */
data class Lease(val jobId: String, val payload: String, val token: Long)
