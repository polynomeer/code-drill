// 검증용 정답 (§6.1 solutions/). 최소 소인수 체 + 수를 소인수와 union.
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) {
        if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }
        i += 1
    }
    val parent = IntArray(limit + 1) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }
    for (x in nums) {
        var v = x
        while (v > 1) {
            val p = spf[v]
            Drill.compare(x, p)
            union(x, p)
            while (v % p == 0) v /= p
        }
    }
    val counts = HashMap<Int, Int>()
    var best = 0
    for (x in nums) {
        val root = find(x)
        val size = (counts[root] ?: 0) + 1
        counts[root] = size
        Drill.write(root, size)
        if (size > best) best = size
    }
    return best
}
