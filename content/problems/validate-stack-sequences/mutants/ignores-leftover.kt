// kind: MISSING_EDGE_CASE
// 다 넣은 뒤 꺼내지 못하고 남은 값이 있는지 보지 않는다. popped 를 끝까지 꺼냈어야 한다.
fun validateStackSequences(pushed: IntArray, popped: IntArray): Int {
    val stack = ArrayDeque<Int>()
    var j = 0
    for (x in pushed) {
        stack.addLast(x)
        while (stack.isNotEmpty() && j < popped.size && stack.last() == popped[j]) { stack.removeLast(); j += 1 }
    }
    return 1
}
