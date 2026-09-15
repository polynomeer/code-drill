// 검증용 정답 (§6.1 solutions/). 오른쪽으로 가는 것을 쌓고, 왼쪽으로 가는 것이 오면 견준다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            Drill.compare(top, size)
            when {
                top < -size -> { stack.removeLast(); Drill.pop(top) }
                top == -size -> { stack.removeLast(); Drill.pop(top); alive = false }
                else -> alive = false
            }
        }
        if (alive) { stack.addLast(size); Drill.push(size) }
    }
    return stack.toIntArray()
}
