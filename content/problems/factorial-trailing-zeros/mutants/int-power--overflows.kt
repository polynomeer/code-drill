// kind: MISSING_EDGE_CASE
// 5 의 거듭제곱을 Int 로 곱한다. 큰 n 에서 넘친 값이 양수로 돌아와 엉뚱한 몫을 더한다.
fun trailingZeros(n: Int): Int {
    var count = 0
    var power = 5
    while (power <= n && power > 0) { count += n / power; power *= 5 }
    return count
}
