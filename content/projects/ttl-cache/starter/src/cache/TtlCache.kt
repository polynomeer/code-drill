package cache

/**
 * 만료가 있는 LRU 캐시 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 *
 * 시간은 [clock] 에서만 읽는다.
 */
class TtlCache<K, V>(
    val capacity: Int,
    val ttlMillis: Long,
    private val clock: () -> Long,
) {
    init {
        TODO("용량과 만료 시간을 검사한다")
    }

    fun put(key: K, value: V) {
        TODO()
    }

    fun get(key: K): V? {
        TODO()
    }

    fun remove(key: K): Boolean {
        TODO()
    }

    val size: Int
        get() = TODO()

    fun keys(): List<K> {
        TODO()
    }
}
