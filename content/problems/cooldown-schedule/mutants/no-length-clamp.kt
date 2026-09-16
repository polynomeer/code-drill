// kind: MISSING_EDGE_CASE
// 작업이 뼈대보다 많을 때도 뼈대 길이를 답한다. 작업 수보다 작은 답이 나온다.
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    val ties = counts.count { it == most }
    return (most - 1) * (n + 1) + ties
}
