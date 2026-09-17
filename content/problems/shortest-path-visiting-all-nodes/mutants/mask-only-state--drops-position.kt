// kind: WRONG_ALGORITHM
// 상태를 집합만으로 둔다. 어디 서 있는지를 잊어 이어질 수 없는 걸음을 잇는다.
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray(1 shl n)
    val queue = IntArray(1 shl n)
    var head = 0; var tail = 0
    for (s in 0 until n) { if (!seen[1 shl s]) { seen[1 shl s] = true; queue[tail++] = 1 shl s } }
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val mask = queue[head++]
            for (u in 0 until n) {
                if (mask and (1 shl u) == 0) continue
                for (v in adj[u]) {
                    val nm = mask or (1 shl v)
                    if (nm == full) return distance + 1
                    if (!seen[nm]) { seen[nm] = true; queue[tail++] = nm }
                }
            }
        }
        distance += 1
    }
    return -1
}
