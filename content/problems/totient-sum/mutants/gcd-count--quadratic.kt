// kind: PERFORMANCE
// 수마다 1..k 의 gcd 를 센다. O(n² log n).
fun totientSum(n: Int): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    var total = 0L
    for (k in 1..n) {
        var phi = 0
        for (i in 1..k) { Drill.compare(i, k); if (gcd(i, k) == 1) phi += 1 }
        total += phi
    }
    return (total % 1_000_000_007L).toInt()
}
