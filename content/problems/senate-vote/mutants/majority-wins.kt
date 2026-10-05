// kind: WRONG_ALGORITHM
// 의원이 많은 정당이 이긴다고 본다. 먼저 움직이는 쪽이 수를 뒤집을 수 있다.
fun senateVote(senate: String): String {
    val r = senate.count { it == 'R' }
    return if (r * 2 > senate.length || (r * 2 == senate.length && senate[0] == 'R')) "Radiant" else "Dire"
}
