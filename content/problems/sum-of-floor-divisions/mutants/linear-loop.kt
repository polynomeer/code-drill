// kind: PERFORMANCE
// n 번 나눈다. 10 억 번이다.
fun sumOfFloorDivisions(n: Int): Int {
    val mod = 1_000_000_007L
    var total = 0L
    for (i in 1..n) { Drill.compare(i, n); total = (total + n / i) % mod }
    return total.toInt()
}
