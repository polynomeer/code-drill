// 검증용 정답 (§6.1 solutions/). 단조 스택으로 최솟값을 맡는 범위를 구하고, 누적합의 누적합으로 구간 합의 합을 상수 시간에 낸다.
fun wizardStrengthSum(strength: IntArray): Int {
    val mod = 1_000_000_007L
    val n = strength.size
    val left = IntArray(n) { -1 }
    val right = IntArray(n) { n }
    val stack = IntArray(n)
    var size = 0
    for (i in 0 until n) {
        // 왼쪽은 더 작은 것, 오른쪽은 작거나 같은 것 — 같은 값은 왼쪽 원소가 맡는다
        while (size > 0 && strength[stack[size - 1]] >= strength[i]) { right[stack[size - 1]] = i; Drill.pop(stack[size - 1]); size -= 1 }
        left[i] = if (size > 0) stack[size - 1] else -1
        stack[size++] = i
        Drill.push(i)
    }
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = (prefix[i] + strength[i]) % mod
    val prefix2 = LongArray(n + 2)
    for (i in 0..n) prefix2[i + 1] = (prefix2[i] + prefix[i]) % mod
    var total = 0L
    for (i in 0 until n) {
        val l = left[i]; val r = right[i]
        val rightSum = (prefix2[r + 1] - prefix2[i + 1] + mod) % mod
        val leftSum = (prefix2[i + 1] - prefix2[l + 1] + mod) % mod
        val span = ((i - l).toLong() * rightSum % mod - (r - i).toLong() * leftSum % mod + mod) % mod
        total = (total + strength[i] % mod * span) % mod
    }
    return total.toInt()
}
