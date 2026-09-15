// kind: PERFORMANCE
// 2 부터 n 까지 전부 나눠 본다. 수 하나에 10⁹ 번.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L
        var p = 2
        while (p <= n) {
            if (n % p == 0) { total += p; while (n % p == 0) n /= p; Drill.compare(p, n) }
            p += 1
        }
        out[i] = total.toInt()
    }
    return out
}
