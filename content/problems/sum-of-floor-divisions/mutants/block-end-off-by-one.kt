// kind: OFF_BY_ONE
// 구간의 끝을 ⌊n/q⌋ + 1 로 잡는다. 다음 구간의 첫 i 에 큰 몫을 준다.
fun sumOfFloorDivisions(n: Int): Int {
    val mod = 1_000_000_007L
    var total = 0L
    var i = 1L
    while (i <= n) {
        val q = n / i
        val j = minOf(n.toLong(), n / q + 1)
        total = (total + q % mod * ((j - i + 1) % mod)) % mod
        i = j + 1
    }
    return total.toInt()
}
