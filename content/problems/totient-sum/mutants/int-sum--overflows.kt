// kind: WRONG_BRANCH
// 합을 Int 로 다 더한 뒤 나머지를 취한다. 10 만 근처에서 넘친다.
fun totientSum(n: Int): Int {
    val phi = IntArray(n + 1) { it }
    for (p in 2..n) {
        if (phi[p] != p) continue
        var k = p
        while (k <= n) { phi[k] -= phi[k] / p; k += p }
    }
    var total = 0
    for (k in 1..n) total += phi[k]
    return total % 1_000_000_007
}
