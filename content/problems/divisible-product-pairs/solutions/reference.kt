// 검증용 정답 (§6.1 solutions/). 원소를 gcd(v, k) 로 무리 짓고, gcd 곱이 k 의 배수인 무리 쌍마다 센다.
fun divisibleProductPairs(nums: IntArray, k: Int): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    val counts = HashMap<Int, Long>()
    for (v in nums) { val g = gcd(v, k); counts[g] = (counts[g] ?: 0L) + 1 }
    val keys = counts.keys.toIntArray()
    var total = 0L
    for (i in keys.indices) {
        val g = keys[i]; val cg = counts[g]!!
        Drill.write(g, cg.toInt())
        for (j in i until keys.size) {
            val h = keys[j]
            if (g.toLong() * h % k != 0L) continue
            total += if (i == j) cg * (cg - 1) / 2 else cg * counts[h]!!
        }
    }
    return (total % 1_000_000_007).toInt()
}
