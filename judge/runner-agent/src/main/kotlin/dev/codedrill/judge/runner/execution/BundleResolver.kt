package dev.codedrill.judge.runner.execution

import dev.codedrill.judge.protocol.BundleRef
import dev.codedrill.judge.protocol.Bundles
import dev.codedrill.judge.protocol.ExecutionRequest
import dev.codedrill.judge.protocol.RequestedGroup
import dev.codedrill.platform.storage.BlobStore
import org.slf4j.LoggerFactory

/**
 * 요청이 가리키는 번들을 받아 케이스로 푼다 (기술 설계서 §8.3, §4.1).
 *
 * 받은 것의 SHA-256 이 요청의 digest 와 같아야 쓴다. 스토어가 바뀌었든 누가 손댔든,
 * 요청이 말한 것과 다른 테스트로 채점하지 않는다 — 그것은 판정이 아니라 시스템 오류다.
 *
 * 최근 번들은 기억한다. 같은 문제에 제출이 몰리는 것이 보통이고, 매번 수 MB 를 다시
 * 받을 이유가 없다. 키가 내용이라 기억한 것이 낡을 일은 없다.
 */
class BundleResolver(private val store: BlobStore, private val cacheSize: Int = CACHE_SIZE) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val cache = object : LinkedHashMap<String, List<RequestedGroup>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<RequestedGroup>>) = size > cacheSize
    }

    /** 번들이 없으면 요청 그대로다. 케이스가 메시지에 실려 온 것이다. */
    fun resolve(request: ExecutionRequest): ExecutionRequest {
        val bundle = request.bundle ?: return request
        val groups = groupsOf(bundle)
        val selected = request.selection?.let { wanted ->
            val keys = wanted.map { it.groupId to it.caseId }.toSet()
            groups.map { group -> group.copy(cases = group.cases.filter { (group.policy.id to it.id) in keys }) }
                .filter { it.cases.isNotEmpty() }
        } ?: groups
        return request.copy(groups = selected, bundle = null, selection = null)
    }

    private fun groupsOf(bundle: BundleRef): List<RequestedGroup> {
        synchronized(cache) { cache[bundle.digest] }?.let { return it }
        val bytes = store.get(bundle.key)
            ?: throw IllegalStateException("테스트 번들이 스토어에 없다: ${bundle.key}")
        val actual = Bundles.digest(bytes)
        check(actual == bundle.digest) {
            "테스트 번들의 digest 가 요청과 다르다: ${bundle.key} — 요청 ${bundle.digest.take(12)}, 받은 것 ${actual.take(12)}"
        }
        val groups = Bundles.decode(bytes)
        synchronized(cache) { cache[bundle.digest] = groups }
        log.info("테스트 번들을 받았다: {} ({}KB, 그룹 {}개)", bundle.key, bytes.size / 1024, groups.size)
        return groups
    }

    companion object {
        /** 번들 하나가 몇 MB 다. 열여섯 개면 활발한 문제는 다 들어가고 메모리는 수십 MB 다. */
        const val CACHE_SIZE = 16

        /** 번들을 쓰지 않는 곳(테스트, 콘텐츠 검증)용. 번들 요청이 오면 실패한다. */
        val NONE = BundleResolver(
            object : BlobStore {
                override fun put(key: String, bytes: ByteArray, contentType: String, digest: String?) = error("스토어가 없다")
                override fun get(key: String): ByteArray? = null
                override fun digestOf(key: String): String? = null
            },
        )
    }
}
