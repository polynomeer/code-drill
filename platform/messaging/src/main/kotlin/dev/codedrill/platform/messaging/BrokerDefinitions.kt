package dev.codedrill.platform.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

/**
 * 배포용 브로커 정의 (기술 설계서 §11.2 최소 권한, §10.2 큐 토폴로지).
 *
 * 배포에서는 앱이 큐를 선언하지 않는다 — 브로커가 이 정의를 기동 때 읽어 토폴로지와
 * 사용자·권한을 갖는다. 그러면 앱에는 선언(=삭제) 권한이 없어도 되고, 각 신원은 자기
 * 출구 exchange 에 쓰고 자기 큐만 읽는다.
 *
 * **소스는 코드다.** [AmqpConfig] 가 개발에서 선언하는 것과 이 파일이 같아야 하므로,
 * 파일을 손으로 고치지 않고 여기서 만든다 (`./gradlew :platform:messaging:writeBrokerDefinitions`).
 * 테스트가 저장소의 파일과 대조하므로, 큐를 더하고 파일을 안 만들면 빌드가 실패한다.
 */
object BrokerDefinitions {

    /** 운영자용 계정은 여기 없다. 이미지의 기본 사용자(비밀번호는 환경변수)가 그 자리다. */
    fun render(): String {
        val definitions = linkedMapOf(
            "rabbit_version" to "4.0.0",
            "users" to JudgeIdentity.entries.map { identity ->
                linkedMapOf(
                    "name" to identity.id,
                    // 비밀번호가 없다. 인증서로만 들어온다 (AmqpConfig.certificateLogin).
                    "password_hash" to "",
                    "hashing_algorithm" to "rabbit_password_hashing_sha256",
                    "tags" to emptyList<String>(),
                )
            },
            "vhosts" to listOf(mapOf("name" to VHOST)),
            "permissions" to JudgeIdentity.entries.map { identity ->
                val access = accessOf(identity)
                linkedMapOf(
                    "user" to identity.id,
                    "vhost" to VHOST,
                    "configure" to access.configure,
                    "write" to access.write,
                    "read" to access.read,
                )
            },
            "exchanges" to buildList {
                add(exchange(JudgeQueues.DEAD_EXCHANGE, "direct"))
                JudgeIdentity.entries.forEach { add(exchange(it.exchange, "direct")) }
                add(exchange(JudgeQueues.SUBMISSION_EVENTS, "fanout"))
            },
            "queues" to buildList {
                for (name in JudgeQueues.all) {
                    add(
                        queue(
                            name,
                            mapOf(
                                "x-queue-type" to "quorum",
                                "x-delivery-limit" to AmqpConfig.DELIVERY_LIMIT,
                                "x-dead-letter-exchange" to JudgeQueues.DEAD_EXCHANGE,
                                "x-dead-letter-routing-key" to name,
                            ),
                        ),
                    )
                    add(
                        queue(
                            JudgeQueues.dead(name),
                            mapOf("x-queue-type" to "quorum", "x-message-ttl" to AmqpConfig.DEAD_TTL_MILLIS),
                        ),
                    )
                }
            },
            "bindings" to buildList {
                for (lane in JudgeTopology.lanes) {
                    add(binding(lane.from.exchange, lane.queue))
                    add(binding(JudgeQueues.DEAD_EXCHANGE, JudgeQueues.dead(lane.queue), routingKey = lane.queue))
                }
            },
        )
        return mapper.writeValueAsString(definitions) + "\n"
    }

    /** configure / write / read 의 정규식. RabbitMQ 는 이름 전체가 맞아야 허용한다. */
    data class Access(val configure: String, val write: String, val read: String)

    fun accessOf(identity: JudgeIdentity): Access {
        val reads = JudgeTopology.consumedBy(identity).toMutableList()
        val writes = mutableListOf(identity.exchange)
        val configures = mutableListOf<String>()
        if (identity == JudgeIdentity.CONTROL_PLANE) {
            // 치워진 제출을 SYSTEM_ERROR 로 끝맺는 유일한 dead 큐 소비자.
            reads += JudgeQueues.SUBMISSIONS_DEAD
            // SSE 팬아웃: 인스턴스마다 익명 큐를 만들고(configure) 그것을 exchange 에 묶는다 —
            // 묶기는 큐에 write, exchange 에 read 를 본다. exchange 자체는 여기 정의에 있다.
            configures += ANONYMOUS
            writes += listOf(JudgeQueues.SUBMISSION_EVENTS, ANONYMOUS)
            reads += listOf(JudgeQueues.SUBMISSION_EVENTS, ANONYMOUS)
        }
        return Access(configure = anyOf(configures), write = anyOf(writes), read = anyOf(reads))
    }

    private fun anyOf(names: List<String>): String =
        if (names.isEmpty()) "^$" else names.joinToString("|", prefix = "^(", postfix = ")$") { pattern(it) }

    private fun pattern(name: String) =
        if (name == ANONYMOUS) "spring\\.gen-.*" else name.replace(".", "\\.")

    private fun exchange(name: String, type: String) = linkedMapOf(
        "name" to name, "vhost" to VHOST, "type" to type,
        "durable" to true, "auto_delete" to false, "internal" to false, "arguments" to emptyMap<String, Any>(),
    )

    private fun queue(name: String, arguments: Map<String, Any>) = linkedMapOf(
        "name" to name, "vhost" to VHOST, "durable" to true, "auto_delete" to false, "arguments" to arguments,
    )

    private fun binding(source: String, queue: String, routingKey: String = queue) = linkedMapOf(
        "source" to source, "vhost" to VHOST, "destination" to queue, "destination_type" to "queue",
        "routing_key" to routingKey, "arguments" to emptyMap<String, Any>(),
    )

    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    private const val VHOST = "/"

    /**
     * 익명 큐의 자리표. 정규식으로 바뀐다.
     *
     * 이름은 브로커가 아니라 **Spring 이** 짓는다 (`spring.gen-…`). 브로커가 짓는
     * `amq.gen-…` 으로 적었다가 첫 기동에서 configure 거부를 받았다.
     */
    private const val ANONYMOUS = "<anonymous>"
}

/** `./gradlew :platform:messaging:writeBrokerDefinitions` 의 진입점. */
fun main(args: Array<String>) {
    val target = java.nio.file.Path.of(args.single())
    java.nio.file.Files.writeString(target, BrokerDefinitions.render())
    println("wrote $target")
}
