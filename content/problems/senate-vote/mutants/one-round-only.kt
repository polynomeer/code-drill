// kind: WRONG_ALGORITHM
// 한 바퀴만 돌고 남은 수로 정한다. 막는 일은 한 정당이 다 사라질 때까지 이어진다.
fun senateVote(senate: String): String {
    var banR = 0; var banD = 0
    var aliveR = 0; var aliveD = 0
    for (c in senate) {
        if (c == 'R') { if (banR > 0) banR -= 1 else { aliveR += 1; banD += 1 } }
        else { if (banD > 0) banD -= 1 else { aliveD += 1; banR += 1 } }
    }
    return if (aliveR >= aliveD) "Radiant" else "Dire"
}
