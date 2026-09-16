// kind: MISSING_EDGE_CASE
// 전부 9 일 때 자릿수를 늘리지 않는다. 0 들만 남는다.
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    for (i in out.indices.reversed()) {
        if (out[i] < 9) { out[i] += 1; return out }
        out[i] = 0
    }
    return out
}
