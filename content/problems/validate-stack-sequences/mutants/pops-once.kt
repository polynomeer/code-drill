// kind: WRONG_BRANCH
// 값을 하나 넣을 때마다 많아야 하나만 꺼낸다. 맨 위가 계속 꺼낼 차례면 연달아 꺼내야 한다.
fun validateStackSequences(pushed: IntArray, popped: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var j = 0
    for (x in pushed) {
        stack.addLast(x)
        if (stack.isNotEmpty() && j < popped.size && stack.last() == popped[j]) { stack.removeLast(); j += 1 }
    }
    while (stack.isNotEmpty() && j < popped.size && stack.last() == popped[j]) { stack.removeLast(); j += 1 }
    return if (j == popped.size) 1 else 0
}
