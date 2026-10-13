// kind: OFF_BY_ONE
// 같은 무리 안의 쌍을 c × c 로 센다. 자기 자신과 짝짓고 순서까지 두 번 센다 — c(c−1)/2 다.
fun divisibleProductPairs(nums: IntArray, k: Int): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    val counts = HashMap<Int, Long>()
    for (v in nums) { val g = gcd(v, k); counts[g] = (counts[g] ?: 0L) + 1 }
    val keys = counts.keys.toIntArray(); var total = 0L
    for (i in keys.indices) for (j in i until keys.size) {
        if (keys[i].toLong() * keys[j] % k != 0L) continue
        total += counts[keys[i]]!! * counts[keys[j]]!!
    }
    return (total % 1_000_000_007).toInt()
}
