// kind: WRONG_ALGORITHM
// 도로 하나로 닿는 도시만 센다. 여러 도로를 이어 문턱 안에 닿는 도시도 이웃이다.
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val near = Array(n) { BooleanArray(n) }
    for (e in edges.indices step 3) if (edges[e + 2] <= threshold) { near[edges[e]][edges[e + 1]] = true; near[edges[e + 1]][edges[e]] = true }
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        val count = near[i].count { it }
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
