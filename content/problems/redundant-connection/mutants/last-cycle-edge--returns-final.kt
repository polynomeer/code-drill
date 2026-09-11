// kind: WRONG_BRANCH
// 순환을 찾아도 멈추지 않고 마지막 것을 돌려준다. 순환이 둘이면 틀린다.
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var found = intArrayOf(-1, -1)
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        val ra = find(a); val rb = find(b)
        if (ra == rb) found = intArrayOf(a, b) else parent[ra] = rb
        i += 2
    }
    return found
}
