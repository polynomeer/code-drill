// 검증용 정답 (§6.1 solutions/). 시각순 정렬 뒤 유니온파인드로 무리 수를 센다.
fun earliestAllConnected(n: Int, logs: IntArray): Int {
    if (n == 1) return 0
    val m = logs.size / 3
    val order = (0 until m).sortedBy { logs[it * 3] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    var groups = n
    for (k in order) {
        val t = logs[k * 3]; val a = logs[k * 3 + 1]; val b = logs[k * 3 + 2]
        val ra = find(a); val rb = find(b)
        Drill.compare(ra, rb)
        if (ra != rb) {
            parent[ra] = rb
            Drill.write(ra, rb)
            groups -= 1
            if (groups == 1) return t
        }
    }
    return -1
}
