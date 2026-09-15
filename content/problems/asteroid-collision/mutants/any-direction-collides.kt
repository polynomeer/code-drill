// kind: WRONG_ALGORITHM
// 방향을 보지 않고 이웃끼리 부딪힌다고 본다. 같은 방향은 만나지 않는다.
fun asteroids(sizes: IntArray): IntArray {
    val stack = ArrayDeque<Int>()
    for (size in sizes) {
        var alive = true
        while (alive && stack.isNotEmpty()) {
            val top = stack.last()
            if (Math.abs(top) < Math.abs(size)) stack.removeLast()
            else if (Math.abs(top) == Math.abs(size)) { stack.removeLast(); alive = false }
            else alive = false
        }
        if (alive) stack.addLast(size)
    }
    return stack.toIntArray()
}
