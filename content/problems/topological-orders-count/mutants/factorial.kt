// kind: WRONG_ALGORITHM
// 간선을 무시하고 n! 을 낸다. 간선이 순서를 줄인다.
fun topologicalOrdersCount(n: Int, edges: IntArray): Int {
    var f = 1L
    for (i in 2..n) f = f * i % 1_000_000_007
    return f.toInt()
}
