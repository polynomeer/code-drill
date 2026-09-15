// kind: WRONG_ALGORITHM
// 첫 행만 정렬된 것으로 보고 k 번째 원소를 답한다.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val flat = grid[0]
    return flat[minOf(k - 1, flat.size - 1)]
}
