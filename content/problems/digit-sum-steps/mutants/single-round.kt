// kind: MISSING_EDGE_CASE
// 한 번만 더하고 끝낸다. 더한 결과가 또 두 자리일 수 있다.
fun digitSumSteps(n: Int): Int {
    if (n < 10) return 0
    return 1
}
