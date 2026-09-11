// 검증용 정답 (§6.1 solutions/). 닿을 수 있는 가장 먼 칸 하나를 들고 훑는다.
fun canJump(steps: IntArray): Int {
    var reach = 0
    for (i in steps.indices) {
        Drill.visit(i, steps[i])
        if (i > reach) return 0
        if (i + steps[i] > reach) {
            reach = i + steps[i]
            Drill.pointer("reach", reach)
        }
    }
    return 1
}
