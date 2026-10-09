// kind: MISSING_EDGE_CASE
// 순환을 보지 않는다. 꺼내지 못한 수를 순서 끝에 붙여 행렬을 낸다 — 조건을 지킬 수 없으면 빈 배열이다.
fun buildMatrix(k: Int, rowConditions: IntArray, colConditions: IntArray): Array<IntArray> {
    fun order(conditions: IntArray): IntArray? {
        val adj = Array(k + 1) { ArrayList<Int>() }
        val indegree = IntArray(k + 1)
        for (i in conditions.indices step 2) { adj[conditions[i]].add(conditions[i + 1]); indegree[conditions[i + 1]] += 1 }
        val ready = java.util.PriorityQueue<Int>()
        for (v in 1..k) if (indegree[v] == 0) ready.add(v)
        val out = IntArray(k)
        var size = 0
        while (ready.isNotEmpty()) {
            val v = ready.poll()
            out[size++] = v
            Drill.dequeue(v)
            for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
        }
        for (v in 1..k) if (v !in out) { out[size++] = v }
        return out
    }
    val rows = order(rowConditions) ?: return arrayOf()
    val cols = order(colConditions) ?: return arrayOf()
    val rowOf = IntArray(k + 1); val colOf = IntArray(k + 1)
    for (i in 0 until k) { rowOf[rows[i]] = i; colOf[cols[i]] = i }
    val matrix = Array(k) { IntArray(k) }
    for (v in 1..k) matrix[rowOf[v]][colOf[v]] = v
    return matrix
}
