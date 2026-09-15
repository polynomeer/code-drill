// kind: WRONG_BRANCH
// 같은 소인수를 나올 때마다 더한다. 서로 다른 소인수만 더해야 한다.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        var n = value; var total = 0L; var p = 2L
        while (p * p <= n) {
            while (n % p == 0L) { total += p; n = (n / p).toInt() }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
    }
    return out
}
