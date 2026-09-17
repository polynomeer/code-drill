// kind: WRONG_BRANCH
// 무리의 크기를 소수 정점까지 세어 답한다. 배열에 없는 소수가 크기에 들어간다.
fun largestCommonFactorComponent(nums: IntArray): Int {
    val limit = nums.max()
    val spf = IntArray(limit + 1) { it }
    var i = 2
    while (i.toLong() * i <= limit) { if (spf[i] == i) { var k = i * i; while (k <= limit) { if (spf[k] == k) spf[k] = i; k += i } }; i += 1 }
    val parent = IntArray(limit + 1) { it }
    val size = IntArray(limit + 1) { 1 }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; size[rb] += size[ra] } }
    for (x in nums) { var v = x; while (v > 1) { val p = spf[v]; union(x, p); while (v % p == 0) v /= p } }
    var best = 0
    for (x in nums) best = maxOf(best, size[find(x)])
    return best
}
