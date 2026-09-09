// 검증용 정답 (§6.1 solutions/). 스택 기반 flood fill.
//
// 계측은 다섯 렌더러를 모두 쓴다. 판정 실행에서는 전부 no-op 으로 컴파일된다 (§7.1).
//
// 격자는 2차원으로 받지만 **계측 인덱스는 행 우선으로 편 자리**다 (`행 * 열수 + 열`).
// 배열 렌더러가 평탄화된 한 줄을 그리기 때문이며, 그래야 어느 칸을 봤는지가 화면과
// 맞는다.
fun countIslands(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val seen = Array(rows) { BooleanArray(cols) }
    var islands = 0

    for (start in 0 until rows * cols) {
        val startRow = start / cols
        val startColumn = start % cols
        Drill.pointer("scan", start)
        if (grid[startRow][startColumn] == 0 || seen[startRow][startColumn]) continue

        islands += 1
        val label = "flood($start)"
        Drill.call(label)

        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        seen[startRow][startColumn] = true
        Drill.push(start)
        var size = 0

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val row = current / cols
            val column = current % cols
            Drill.pop(current)
            Drill.visit(current, grid[row][column])
            Drill.node("c$current")
            size += 1

            val neighbours = listOf(
                if (row > 0) current - cols else -1,
                if (row < rows - 1) current + cols else -1,
                if (column > 0) current - 1 else -1,
                if (column < cols - 1) current + 1 else -1,
            )
            for (next in neighbours) {
                if (next < 0) continue
                val nextRow = next / cols
                val nextColumn = next % cols
                if (grid[nextRow][nextColumn] == 0 || seen[nextRow][nextColumn]) continue
                seen[nextRow][nextColumn] = true
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
