// kind: MISSING_EDGE_CASE
// 수를 가장 작은 소인수와만 묶는다. 6 과 15 처럼 큰 소인수로만 이어진 쌍이 끊긴다.
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) if (x > 1) union(x, spf[x])
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) { val size = (counts[find(x)] ?: 0) + 1; counts[find(x)] = size; if (size > best) best = size }
    return best
}
