// kind: MISSING_EDGE_CASE
// √n 까지 나눈 뒤 남은 n 을 더하지 않는다. 큰 소인수가 빠진다.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p <= n) {
            if (n % p == 0L) { total += p; while (n % p == 0L) n = (n / p).toInt() }
            p += 1
        }
        out[i] = total.toInt()
    }
    return out
}
