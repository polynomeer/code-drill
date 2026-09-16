// 검증용 정답 (§6.1 solutions/). 가장 많은 작업이 뼈대, 나머지는 틈에 들어간다.
fun leastInterval(tasks: String, n: Int): Int {
    val counts = IntArray(26)
    for (ch in tasks) counts[ch - 'A'] += 1
    var most = 0
    var ties = 0
    for (kind in 0 until 26) {
        Drill.write(kind, counts[kind])
        Drill.compare(counts[kind], most)
        if (counts[kind] > most) { most = counts[kind]; ties = 1 } else if (counts[kind] == most && most > 0) ties += 1
    }
    return maxOf(tasks.length, (most - 1) * (n + 1) + ties)
}
