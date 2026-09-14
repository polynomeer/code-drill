package dev.codedrill.judge.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.security.MessageDigest

/**
 * 테스트 번들의 모양 (기술 설계서 §8.3).
 *
 * 번들은 [RequestedGroup] 목록을 JSON 으로 적은 것이다 — 메시지에 실리던 바로 그것을
 * 오브젝트 스토어로 옮겼을 뿐이다. 쓰는 쪽(오케스트레이터)과 읽는 쪽(Runner)이 이 한
 * 곳을 쓴다. 갈라지면 같은 문제가 노드에 따라 다른 테스트로 채점된다.
 *
 * 키는 패키지 digest 로 정한다. 같은 패키지는 같은 키이고, 내용이 바뀌면 digest 가 바뀌어
 * 키도 바뀐다 — 지난 제출이 그때의 번들로 재현된다 (§12.4).
 */
object Bundles {

    private val json = ObjectMapper().registerKotlinModule()

    fun key(packageDigest: String) = "bundles/$packageDigest.json"

    fun encode(groups: List<RequestedGroup>): ByteArray = json.writeValueAsBytes(groups)

    fun decode(bytes: ByteArray): List<RequestedGroup> = json.readValue(bytes)

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
