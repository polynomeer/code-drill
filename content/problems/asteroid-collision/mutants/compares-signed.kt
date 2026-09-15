// kind: WRONG_BRANCH
// 크기를 절댓값이 아니라 부호 있는 값으로 견준다. 왼쪽으로 가는 것이 늘 진다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            when {
                top < size -> stack.removeLast()
                top == size -> { stack.removeLast(); alive = false }
                else -> alive = false
            }
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
