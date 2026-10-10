package ranges

/** 반열린 구간 `[from, to)`. `from < to` 인 것만 집합에 들어간다. */
data class Span(val from: Long, val to: Long)
