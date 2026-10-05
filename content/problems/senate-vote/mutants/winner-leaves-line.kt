// kind: WRONG_BRANCH
// 상대를 막은 의원을 다시 줄에 세우지 않는다. 그 의원도 다음 바퀴에 차례가 온다.
fun senateVote(senate: String): String {
    val radiant = java.util.ArrayDeque<Int>(); val dire = java.util.ArrayDeque<Int>()
    for (i in senate.indices) if (senate[i] == 'R') radiant.add(i) else dire.add(i)
    while (radiant.isNotEmpty() && dire.isNotEmpty()) {
        val r = radiant.poll(); val d = dire.poll()
        if (r < d) { if (dire.isEmpty()) radiant.add(r) } else { if (radiant.isEmpty()) dire.add(d) }
    }
    return if (radiant.isNotEmpty()) "Radiant" else "Dire"
}
