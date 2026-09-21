package store

// kind: WRONG_ALGORITHM
// 되감기가 이력을 그 버전으로 자르고 버전도 되돌린다. 새 버전이 아니다.

class VersionedStore {
    /** 키마다 (버전, 값) 이력. 값이 null 이면 그 버전에 지워진 것이다. 버전은 오름차순이다. */
    private class Entry(val version: Int, val value: String?)

    private val history = HashMap<String, ArrayList<Entry>>()
    private var current = 0

    val version: Int
        get() = current

    private fun latestAt(key: String, version: Int): String? {
        val entries = history[key] ?: return null
        // 버전 이하의 마지막 항목 — 이분 탐색.
        var lo = 0; var hi = entries.size - 1; var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (entries[mid].version <= version) { found = mid; lo = mid + 1 } else hi = mid - 1
        }
        return if (found < 0) null else entries[found].value
    }

    private fun write(key: String, value: String?) {
        history.getOrPut(key) { ArrayList() }.add(Entry(current, value))
    }

    fun put(key: String, value: String): Int {
        current += 1
        write(key, value)
        return current
    }

    fun delete(key: String): Int {
        if (get(key) == null) throw NoSuchKeyException(key)
        current += 1
        write(key, null)
        return current
    }

    fun get(key: String): String? = latestAt(key, current)

    fun getAt(key: String, version: Int): String? {
        require(version in 0..current) { "version out of range: $version (0..$current)" }
        return latestAt(key, version)
    }

    fun rollback(version: Int): Int {
        require(version in 0..current) { "version out of range: $version (0..$current)" }
        // 그 시점의 상태를 새 버전으로 다시 적는다 — 이력은 자르지 않는다.
        for (entries in history.values) entries.removeAll { it.version > version }
        current = version
        return current
    }

    fun keys(): List<String> = history.keys.filter { latestAt(it, current) != null }.sorted()
}
