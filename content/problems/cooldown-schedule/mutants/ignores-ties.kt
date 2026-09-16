// kind: OFF_BY_ONE
// 가장 많은 종류가 여럿이어도 마지막 줄에 하나만 센다.
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    return maxOf(tasks.length, (most - 1) * (n + 1) + 1)
}
