// kind: MISSING_EDGE_CASE
// 이미 1 인 구간만 재고 뒤집기를 쓰지 않는다.
fun longestOnes(bits: IntArray, k: Int): Int {
    var best = 0
    var run = 0
    for (bit in bits) {
        run = if (bit == 1) run + 1 else 0
        if (run > best) best = run
    }
    return best
}
