package ranges

/**
 * 구간 집합 — 골격. 시그니처는 그대로 두고 본문을 채운다. 필드와 도우미는 마음대로 더한다.
 */
class RangeSet {

    fun add(from: Long, to: Long) {
        TODO()
    }

    fun remove(from: Long, to: Long) {
        TODO()
    }

    fun contains(x: Long): Boolean = TODO()

    fun covers(from: Long, to: Long): Boolean = TODO()

    fun spans(): List<Span> = TODO()

    fun size(): Long = TODO()
}
