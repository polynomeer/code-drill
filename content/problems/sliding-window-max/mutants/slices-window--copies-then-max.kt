// kind: PERFORMANCE
// 창마다 부분 배열을 복사해 최댓값을 구한다. O(n·k) 에 할당까지 — 가장 흔한 첫 풀이다.
fun windowMax(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    for (start in out.indices) {
        val window = nums.copyOfRange(start, start + k)
        for (i in window.indices) Drill.visit(start + i, window[i])
        out[start] = window.max()
    }
    return out
}
