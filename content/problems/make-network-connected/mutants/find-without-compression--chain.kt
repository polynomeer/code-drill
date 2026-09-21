// kind: PERFORMANCE
// 경로 압축도 크기 합치기도 없이 항상 a 를 b 아래에 둔다. 사슬이 길어져 O(n²).
fun makeNetworkConnected(n: Int, cables: IntArray): Int {
    val m = cables.size / 2
    if (m < n - 1) return -1
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { Drill.compare(r, parent[r]); r = parent[r] }; return r }
    var components = n
    for (i in 0 until m) { val a = find(cables[2 * i]); val b = find(cables[2 * i + 1]); if (a != b) { parent[b] = a; components -= 1 } }
    return components - 1
}
