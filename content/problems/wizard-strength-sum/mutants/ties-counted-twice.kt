// kind: MISSING_EDGE_CASE
// 왼쪽과 오른쪽 모두 '더 작은' 원소에서 멈춘다. 같은 값이 여럿인 구간은 최솟값을 맡는 원소가 둘이 되어 두 번 세어진다.
fun wizardStrengthSum(strength: IntArray): Int {
    val mod = 1_000_000_007L
    val n = strength.size
    val left = IntArray(n) { -1 }; val right = IntArray(n) { n }
    val stack = ArrayList<Int>()
    for (i in 0 until n) {
        while (stack.isNotEmpty() && strength[stack.last()] > strength[i]) right[stack.removeAt(stack.size - 1)] = i
        stack.add(i)
    }
    stack.clear()
    for (i in n - 1 downTo 0) {
        while (stack.isNotEmpty() && strength[stack.last()] > strength[i]) left[stack.removeAt(stack.size - 1)] = i
        stack.add(i)
    }
    val prefix = LongArray(n + 1); for (i in 0 until n) prefix[i + 1] = (prefix[i] + strength[i]) % mod
    val prefix2 = LongArray(n + 2); for (i in 0..n) prefix2[i + 1] = (prefix2[i] + prefix[i]) % mod
    var total = 0L
    for (i in 0 until n) {
        val l = left[i]; val r = right[i]
        val rs = (prefix2[r + 1] - prefix2[i + 1] + mod) % mod; val ls = (prefix2[i + 1] - prefix2[l + 1] + mod) % mod
        val span = ((i - l).toLong() * rs % mod - (r - i).toLong() * ls % mod + mod) % mod
        total = (total + strength[i] % mod * span) % mod
    }
    return total.toInt()
}
