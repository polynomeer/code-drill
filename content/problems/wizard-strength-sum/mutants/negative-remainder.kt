// kind: MISSING_EDGE_CASE
// 뺄셈 뒤의 나머지를 양수로 되돌리지 않는다. 음수 나머지가 그대로 더해져 답이 음수나 엉뚱한 수가 된다.
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
    val prefix = LongArray(n + 1); for (i in 0 until n) prefix[i + 1] = (prefix[i] + strength[i]) % mod
    val prefix2 = LongArray(n + 2); for (i in 0..n) prefix2[i + 1] = (prefix2[i] + prefix[i]) % mod
    var total = 0L
    for (i in 0 until n) {
        val l = left[i]; val r = right[i]
        val rs = (prefix2[r + 1] - prefix2[i + 1]) % mod; val ls = (prefix2[i + 1] - prefix2[l + 1]) % mod
        val span = ((i - l).toLong() * rs % mod - (r - i).toLong() * ls % mod) % mod
        total = (total + strength[i] % mod * span) % mod
    }
    return total.toInt()
}
