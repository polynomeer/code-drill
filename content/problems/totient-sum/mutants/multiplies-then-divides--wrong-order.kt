// kind: WRONG_ALGORITHM
// φ(k) 를 k × (p−1) / p 로 원래 k 에서 매번 새로 계산한다. 소인수가 둘 이상이면 틀린다.
fun totientSum(n: Int): Int {
    val phi = IntArray(n + 1) { it }
    for (p in 2..n) {
        if (phi[p] != p) continue
        var k = p
        while (k <= n) { phi[k] = k / p * (p - 1); k += p }
    }
    var total = 0L
    for (k in 1..n) total += phi[k]
    return (total % 1_000_000_007L).toInt()
}
