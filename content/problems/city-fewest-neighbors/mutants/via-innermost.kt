// kind: WRONG_ALGORITHM
// 거쳐 가는 도시를 가장 안쪽 반복에 둔다. 아직 확정되지 않은 거리를 이어 붙여 먼 쌍을 놓친다.
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) { val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]; if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w } }
    for (i in 0 until n) for (j in 0 until n) for (k in 0 until n) if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j]
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] <= threshold) count += 1
        if (count <= best) { best = count; answer = i }
    }
    return answer
}
