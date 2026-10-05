// 검증용 정답 (§6.1 solutions/). 정당마다 번호의 큐. 작은 번호가 상대를 막고 번호 + n 으로 줄 끝에 선다.
fun senateVote(senate: String): String {
    val n = senate.length
    val radiant = java.util.ArrayDeque<Int>()
    val dire = java.util.ArrayDeque<Int>()
    for (i in 0 until n) if (senate[i] == 'R') radiant.add(i) else dire.add(i)
    while (radiant.isNotEmpty() && dire.isNotEmpty()) {
        val r = radiant.poll()
        val d = dire.poll()
        Drill.dequeue(minOf(r, d))
        if (r < d) { radiant.add(r + n); Drill.enqueue(r + n) } else { dire.add(d + n); Drill.enqueue(d + n) }
    }
    return if (radiant.isNotEmpty()) "Radiant" else "Dire"
}
