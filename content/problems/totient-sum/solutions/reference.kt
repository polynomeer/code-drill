// 검증용 정답 (§6.1 solutions/). 소수마다 배수를 걸러 φ 를 한 번에 만든다.
fun totientSum(n: Int): Int {
    val phi = IntArray(n + 1) { it }
    for (p in 2..n) {
        if (phi[p] != p) continue
        var k = p
        while (k <= n) { Drill.compare(p, k); phi[k] -= phi[k] / p; k += p }
    }
    var total = 0L
    for (k in 1..n) { Drill.write(k, phi[k]); total += phi[k] }
    return (total % 1_000_000_007L).toInt()
}
