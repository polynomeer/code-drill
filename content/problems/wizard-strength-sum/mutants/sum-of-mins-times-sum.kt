// kind: WRONG_ALGORITHM
// 최솟값의 합에 전체 합을 곱한다. 세기는 구간마다 그 구간의 최솟값 × 그 구간의 합이다.
fun wizardStrengthSum(strength: IntArray): Int {
    val mod = 1_000_000_007L
    val n = strength.size
    val left = IntArray(n) { -1 }; val right = IntArray(n) { n }
    val stack = IntArray(n); var size = 0
    for (i in 0 until n) {
        while (size > 0 && strength[stack[size - 1]] >= strength[i]) { right[stack[size - 1]] = i; size -= 1 }
        left[i] = if (size > 0) stack[size - 1] else -1
        stack[size++] = i
    }
    var mins = 0L
    for (i in 0 until n) mins = (mins + strength[i] % mod * ((i - left[i]).toLong() * (right[i] - i) % mod)) % mod
    var sum = 0L
    for (v in strength) sum = (sum + v) % mod
    return (mins * sum % mod).toInt()
}
