// kind: PERFORMANCE
// 모든 쌍의 gcd 를 본다. O(n² log).
fun largestCommonFactorComponent(nums: IntArray): Int {
    fun gcd(a: Int, b: Int): Int { var x = a; var y = b; while (y != 0) { val t = x % y; x = y; y = t }; return x }
    val n = nums.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        if (gcd(nums[i], nums[j]) > 1) { val a = find(i); val b = find(j); if (a != b) parent[a] = b }
    }
    val counts = IntArray(n)
    var best = 0
    for (i in 0 until n) { val r = find(i); counts[r] += 1; if (counts[r] > best) best = counts[r] }
    return best
}
