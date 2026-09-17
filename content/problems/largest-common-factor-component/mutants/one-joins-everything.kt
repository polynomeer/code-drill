// kind: MISSING_EDGE_CASE
// 1 을 소인수 1 과 묶고 다른 수도 1 과 묶는다 — 1 이 있으면 전부 한 무리가 된다.
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) { var v = x; while (v >= 1) { val p = spf[v]; union(x, p); if (p == 1) break; while (v % p == 0) v /= p } }
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) { val size = (counts[find(x)] ?: 0) + 1; counts[find(x)] = size; if (size > best) best = size }
    return best
}
