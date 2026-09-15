// kind: OFF_BY_ONE
// 크기가 같을 때 쌓인 것만 부서지고 움직이는 것은 살아남는다. 둘 다 부서져야 한다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && size < 0 && stack.isNotEmpty() && stack.last() > 0) {
            val top = stack.last()
            if (top <= -size) stack.removeLast() else alive = false
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
