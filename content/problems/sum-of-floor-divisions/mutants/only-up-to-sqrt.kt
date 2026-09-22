// kind: MISSING_EDGE_CASE
// i 를 √n 까지만 돈다. 그 뒤의 작은 몫들이 빠진다.
fun sumOfFloorDivisions(n: Int): Int {
    val mod = 1_000_000_007L
    var total = 0L
    var i = 1L
    while (i * i <= n) {
        total = (total + n / i) % mod
        i += 1
    }
    return total.toInt()
}
