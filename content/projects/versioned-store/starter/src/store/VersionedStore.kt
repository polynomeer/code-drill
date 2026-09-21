package store

/**
 * 버전이 있는 키-값 저장소 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 */
class VersionedStore {
    val version: Int
        get() = TODO("지금 버전")

    fun put(key: String, value: String): Int {
        TODO()
    }

    fun delete(key: String): Int {
        TODO()
    }

    fun get(key: String): String? {
        TODO()
    }

    fun getAt(key: String, version: Int): String? {
        TODO()
    }

    fun rollback(version: Int): Int {
        TODO()
    }

    fun keys(): List<String> {
        TODO()
    }
}
