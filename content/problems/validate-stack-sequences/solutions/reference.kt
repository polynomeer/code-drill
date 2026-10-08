// 검증용 정답 (§6.1 solutions/). 실제로 넣고, 맨 위가 꺼낼 차례인 동안 꺼낸다.
fun validateStackSequences(pushed: IntArray, popped: IntArray): Int {
    val stack = IntArray(pushed.size)
    var top = 0
    var j = 0
    for (x in pushed) {
        stack[top] = x
        top += 1
        Drill.push(x)
        while (top > 0 && j < popped.size && stack[top - 1] == popped[j]) {
            top -= 1
            Drill.pop(stack[top])
            j += 1
        }
    }
    return if (j == popped.size) 1 else 0
}
