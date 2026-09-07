// kind: WRONG_BRANCH
// 간선 수가 짝수인지만 본다.
fun isBipartite(n: Int, edges: IntArray): Int =
    if ((edges.size / 2) % 2 == 0) 1 else 0
