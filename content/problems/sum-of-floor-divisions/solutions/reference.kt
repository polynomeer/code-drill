// 검증용 정답 (§6.1 solutions/). 같은 몫의 구간을 한 번에.
fun sumOfFloorDivisions(n: Int): Int {
    val mod = 1_000_000_007L
    var total = 0L
    var i = 1L
    while (i <= n) {
        val q = n / i
        val j = n / q
        Drill.compare(i.toInt(), j.toInt())
        total = (total + q % mod * ((j - i + 1) % mod)) % mod
        Drill.write(0, total.toInt())
        i = j + 1
    }
    return total.toInt()
}
