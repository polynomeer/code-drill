// kind: WRONG_ALGORITHM
// 직접 등식으로 이어진 쌍만 같다고 본다. a==b, b==c 에서 a 와 c 를 다르게 본다.
fun equationsPossible(equations: Array<String>): Int {
    val same = Array(26) { BooleanArray(26) }
    for (i in 0 until 26) same[i][i] = true
    for (eq in equations) if (eq[1] == '=') { same[eq[0] - 'a'][eq[3] - 'a'] = true; same[eq[3] - 'a'][eq[0] - 'a'] = true }
    for (eq in equations) if (eq[1] == '!' && same[eq[0] - 'a'][eq[3] - 'a']) return 0
    return 1
}
