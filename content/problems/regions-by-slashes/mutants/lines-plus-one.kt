// kind: WRONG_ALGORITHM
// 빗금 수에 1 을 더한다. 선분이 테두리나 다른 선분과 만나 닫힐 때만 영역이 는다.
fun regionsBySlashes(grid: Array<String>): Int = 1 + grid.sumOf { row -> row.count { it != ' ' } }
