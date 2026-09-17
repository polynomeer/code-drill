// kind: PERFORMANCE
// 창마다 복사해 정렬한다. O(n·k log k).
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val out = IntArray(nums.size - k + 1)
    for (i in out.indices) {
        val window = nums.copyOfRange(i, i + k)
        window.sort()
        Drill.compare(i, window[k / 2])
        out[i] = window[k / 2]
    }
    return out
}
