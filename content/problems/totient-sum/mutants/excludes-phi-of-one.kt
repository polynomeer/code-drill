// kind: OFF_BY_ONE
// φ(1) 을 더하지 않는다.
fun totientSum(n: Int): Int {
    val phi = IntArray(n + 1) { it }
    for (p in 2..n) {
        if (phi[p] != p) continue
        var k = p
        while (k <= n) { phi[k] -= phi[k] / p; k += p }
    }
    var total = 0L
    for (k in 2..n) total += phi[k]
    return (total % 1_000_000_007L).toInt()
}
