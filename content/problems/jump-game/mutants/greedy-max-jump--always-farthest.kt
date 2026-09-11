// kind: WRONG_ALGORITHM
// 매번 최대로 뛴다. 최대로 뛰면 0 위에 떨어지는 경우를 놓친다.
fun canJump(steps: IntArray): Int {
    var i = 0
    while (i < steps.size - 1) {
        if (steps[i] == 0) return 0
        i += steps[i]
    }
    return 1
}
