package dev.codedrill.platform.messaging

/**
 * 브로커에 붙는 세 신원 (기술 설계서 §11.2 워크로드 신원).
 *
 * 앱마다 하나다. 브로커는 이 이름으로 누가 무엇을 넣고 뺄 수 있는지 가른다 —
 * 인증서의 CN 이 이 이름이고, 브로커 사용자도 이 이름이다.
 */
enum class JudgeIdentity(val id: String) {
    CONTROL_PLANE("control-plane"),
    ORCHESTRATOR("orchestrator"),
    RUNNER("runner"),
    ;

    /**
     * 이 신원이 발행하는 유일한 출구.
     *
     * 큐 이름으로 바로 보내지 않는다. 기본 exchange 로 보내면 브로커는 **어느 큐로 가는지
     * 보지 않고** exchange 하나에 대한 쓰기 권한만 본다 — 실제로 확인했다. 그래서 큐마다
     * 권한을 나눌 수 없고, 결과 큐에 쓸 수 있는 Runner 가 제출 큐에도 쓸 수 있게 된다.
     *
     * 신원마다 exchange 를 하나씩 두고 큐를 거기 묶으면, 쓰기 권한이 곧 "이 신원이 보낼
     * 수 있는 큐의 집합"이 된다. Runner 를 깬 사람은 Runner 의 출구만 얻는다.
     */
    val exchange: String get() = "judge.from.$id"

    companion object {
        fun of(id: String): JudgeIdentity = entries.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException(
                "알 수 없는 브로커 신원 '$id'. 가능한 값: ${entries.joinToString { it.id }}",
            )
    }
}

/** 큐 하나의 방향: 누가 넣고 누가 빼는가. */
data class Lane(val queue: String, val from: JudgeIdentity, val to: JudgeIdentity)

/**
 * 채점 경로의 방향표 (§2.3 신뢰 경계, §11.2).
 *
 * [JudgeQueues] 가 큐의 이름과 이유를 적는다면, 여기는 **누가 어느 쪽으로** 쓰는지를
 * 적는다. 브로커 권한과 exchange 바인딩이 여기서 나온다 — 표를 고치면 둘이 함께 바뀌고,
 * 한쪽만 고칠 길이 없다.
 */
object JudgeTopology {
    private val cp = JudgeIdentity.CONTROL_PLANE
    private val orc = JudgeIdentity.ORCHESTRATOR
    private val runner = JudgeIdentity.RUNNER

    val lanes: List<Lane> = listOf(
        Lane(JudgeQueues.SUBMISSIONS, from = cp, to = orc),
        Lane(JudgeQueues.EXECUTIONS, from = orc, to = runner),
        Lane(JudgeQueues.RESULTS, from = runner, to = orc),
        Lane(JudgeQueues.PROGRESS, from = orc, to = cp),
        Lane(JudgeQueues.HEARTBEATS, from = runner, to = orc),
        Lane(JudgeQueues.TRIALS, from = cp, to = runner),
        Lane(JudgeQueues.TRIAL_RESULTS, from = runner, to = cp),
        Lane(JudgeQueues.MUTATIONS, from = cp, to = runner),
        Lane(JudgeQueues.MUTATION_RESULTS, from = runner, to = cp),
        Lane(JudgeQueues.REFERENCE_TRACES, from = cp, to = runner),
        Lane(JudgeQueues.REFERENCE_TRACE_RESULTS, from = runner, to = cp),
        Lane(JudgeQueues.SHRINKS, from = cp, to = runner),
        Lane(JudgeQueues.SHRINK_RESULTS, from = runner, to = cp),
        Lane(JudgeQueues.LABS, from = cp, to = runner),
        Lane(JudgeQueues.LAB_RESULTS, from = runner, to = cp),
        Lane(JudgeQueues.ARENA, from = cp, to = runner),
        Lane(JudgeQueues.ARENA_RESULTS, from = runner, to = cp),
    )

    init {
        val listed = lanes.map { it.queue }
        require(listed.toSet() == JudgeQueues.all.toSet() && listed.size == JudgeQueues.all.size) {
            "JudgeQueues.all 과 JudgeTopology.lanes 가 다르다. 큐를 더했으면 방향도 적는다."
        }
    }

    fun publishedBy(identity: JudgeIdentity) = lanes.filter { it.from == identity }.map { it.queue }
    fun consumedBy(identity: JudgeIdentity) = lanes.filter { it.to == identity }.map { it.queue }
}
