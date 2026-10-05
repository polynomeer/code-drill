// kind: WRONG_BRANCH
// 다음 바퀴의 줄에 설 때 번호를 그대로 둔다. 다음 바퀴의 차례는 이번 바퀴의 모든 의원 뒤다.
fun senateVote(senate: String): String {
    val radiant = java.util.PriorityQueue<Int>(); val dire = java.util.PriorityQueue<Int>()
    for (i in senate.indices) if (senate[i] == 'R') radiant.add(i) else dire.add(i)
    while (radiant.isNotEmpty() && dire.isNotEmpty()) {
        val r = radiant.poll(); val d = dire.poll()
        if (r < d) radiant.add(r) else dire.add(d)
    }
    return if (radiant.isNotEmpty()) "Radiant" else "Dire"
}
