// kind: MISSING_EDGE_CASE
// 약수 둘의 곱을 Int 로 한다. k 가 10 만에 가까우면 k² 이 Int 를 넘어 엉뚱한 나머지가 나온다.
fun divisibleProductPairs(nums: IntArray, k: Int): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    val counts = HashMap<Int, Long>()
    for (v in nums) { val g = gcd(v, k); counts[g] = (counts[g] ?: 0L) + 1 }
    val keys = counts.keys.toIntArray(); var total = 0L
    for (i in keys.indices) for (j in i until keys.size) {
        if (keys[i] * keys[j] % k != 0) continue
        val c = counts[keys[i]]!!
        total += if (i == j) c * (c - 1) / 2 else c * counts[keys[j]]!!
    }
    return (total % 1_000_000_007).toInt()
}
