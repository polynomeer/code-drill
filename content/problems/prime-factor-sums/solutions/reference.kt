// 검증용 정답 (§6.1 solutions/). √n 까지의 시험 나눗셈.
fun primeFactorSums(nums: IntArray): IntArray {
    val out = IntArray(nums.size)
    for ((i, value) in nums.withIndex()) {
        Drill.visit(i, value)
        var n = value
        var total = 0L
        var p = 2L
        while (p * p <= n) {
            if (n % p == 0L) {
                total += p
                while (n % p == 0L) n = (n / p).toInt()
            }
            p += 1
        }
        if (n > 1) total += n
        out[i] = total.toInt()
        Drill.write(i, out[i])
    }
    return out
}
