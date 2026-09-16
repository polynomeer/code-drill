package cache

// kind: MISSING_EDGE_CASE
// 가득 찼을 때 만료된 것을 먼저 쓸어 내지 않는다. 만료된 항목 때문에 멀쩡한 항목이 나간다.
class TtlCache<K, V>(
    val capacity: Int,
    val ttlMillis: Long,
    private val clock: () -> Long,
) {
    private class Entry<V>(var value: V, var expiresAt: Long)

    // accessOrder = true: get 이 항목을 뒤로 보낸다. 앞이 가장 오래 쓰지 않은 것이다.
    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    init {
        require(capacity >= 1) { "capacity must be at least 1: $capacity" }
        require(ttlMillis >= 1) { "ttlMillis must be at least 1: $ttlMillis" }
    }

    private fun expired(entry: Entry<V>, now: Long) = now >= entry.expiresAt

    private fun sweep(now: Long) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            if (expired(iterator.next().value, now)) iterator.remove()
        }
    }

    fun put(key: K, value: V) {
        val now = clock()
        val existing = entries[key]
        if (existing != null) {
            existing.value = value
            existing.expiresAt = now + ttlMillis
            return
        }
        if (entries.size >= capacity) {
            val oldest = entries.keys.iterator()
            oldest.next()
            oldest.remove()
        }
        entries[key] = Entry(value, now + ttlMillis)
    }

    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (expired(entry, clock())) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    fun remove(key: K): Boolean = entries.remove(key) != null

    val size: Int
        get() {
            sweep(clock())
            return entries.size
        }

    fun keys(): List<K> {
        sweep(clock())
        return entries.keys.toList()
    }
}
