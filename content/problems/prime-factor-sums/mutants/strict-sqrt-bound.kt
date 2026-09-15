// kind: OFF_BY_ONE
// p · p < n 인 동안만 나눈다. n 이 소수의 제곱이면 그 소수를 못 보고 n 자체를 더한다.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p < n) {
            if (n % p == 0L) { total += p; while (n % p == 0L) n = (n / p).toInt() }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
    }
    return out
}
