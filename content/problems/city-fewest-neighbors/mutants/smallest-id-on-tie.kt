// kind: WRONG_BRANCH
// 이웃 수가 같으면 번호가 작은 도시를 고른다. 같으면 가장 큰 번호다.
fun cityFewestNeighbors(n: Int, edges: IntArray, threshold: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = Array(n) { i -> IntArray(n) { j -> if (i == j) 0 else inf } }
    for (e in edges.indices step 3) { val a = edges[e]; val b = edges[e + 1]; val w = edges[e + 2]; if (w < dist[a][b]) { dist[a][b] = w; dist[b][a] = w } }
    for (k in 0 until n) for (i in 0 until n) for (j in 0 until n) if (dist[i][k] + dist[k][j] < dist[i][j]) dist[i][j] = dist[i][k] + dist[k][j]
    var best = Int.MAX_VALUE; var answer = -1
    for (i in 0 until n) {
        var count = 0
        for (j in 0 until n) if (j != i && dist[i][j] <= threshold) count += 1
        if (count < best) { best = count; answer = i }
    }
    return answer
}
