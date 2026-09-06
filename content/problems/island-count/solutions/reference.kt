// 검증용 정답 (§6.1 solutions/). 스택 기반 flood fill.
//
// 계측은 다섯 렌더러를 모두 쓴다. 판정 실행에서는 전부 no-op 으로 컴파일된다 (§7.1).
fun countIslands(grid: IntArray, width: Int): Int {
    val height = grid.size / width
    val seen = BooleanArray(grid.size)
    var islands = 0

    for (start in grid.indices) {
        Drill.pointer("scan", start)
        if (grid[start] == 0 || seen[start]) continue

        islands += 1
        val label = "flood($start)"
        Drill.call(label)

        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        seen[start] = true
        Drill.push(start)
        var size = 0

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            Drill.pop(current)
            Drill.visit(current, grid[current])
            Drill.node("c$current")
            size += 1

            val row = current / width
            val column = current % width
            val neighbours = listOf(
                if (row > 0) current - width else -1,
                if (row < height - 1) current + width else -1,
                if (column > 0) current - 1 else -1,
                if (column < width - 1) current + 1 else -1,
            )
            for (next in neighbours) {
                if (next < 0 || grid[next] == 0 || seen[next]) continue
                seen[next] = true
                Drill.edge("c$current", "c$next")
                Drill.enqueue(next)
                Drill.dequeue(next)
                stack.addLast(next)
                Drill.push(next)
            }
        }

        Drill.ret(label, size)
        Drill.match(start, start)
    }
    return islands
}
