// 검증용 정답 (§6.1 solutions/). 외톨이 후보를 들어온 순서로 큐에 두고, 맨 앞이 더는 외톨이가 아니면 빼기를 되풀이한다.
fun firstUniqueStream(s: String): String {
    val counts = IntArray(26)
    val waiting = ArrayDeque<Char>()
    val out = StringBuilder()
    for (c in s) {
        counts[c - 'a'] += 1
        waiting.addLast(c)
        Drill.enqueue(c - 'a')
        while (waiting.isNotEmpty() && counts[waiting.first() - 'a'] > 1) {
            Drill.dequeue(waiting.first() - 'a')
            waiting.removeFirst()
        }
        out.append(if (waiting.isEmpty()) '#' else waiting.first())
    }
    return out.toString()
}
