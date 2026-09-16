// kind: OFF_BY_ONE
// 틈 하나를 n 칸으로 센다. 같은 작업 사이는 n 칸이지만 작업 자체까지 n+1 칸이 한 주기다.
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    val most = counts.max()
    val ties = counts.count { it == most }
    return maxOf(tasks.length, (most - 1) * n + ties)
}
