// kind: MISSING_EDGE_CASE
// 왼쪽으로 가는 것이 이겨도 하나만 부수고 멈춘다. 연달아 부수지 못한다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        if (size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            when {
                top < -size -> stack.removeLast()
                top == -size -> { stack.removeLast(); alive = false }
                else -> alive = false
            }
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
