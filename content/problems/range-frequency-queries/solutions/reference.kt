// 검증용 정답 (§6.1 solutions/). 값마다 자리 목록, 질의마다 이분 탐색 둘.
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val positions = HashMap<Int, MutableList<Int>>()
    for (i in nums.indices) positions.getOrPut(nums[i]) { ArrayList() }.add(i)
    fun lowerBound(list: List<Int>, target: Int): Int {
        var lo = 0; var hi = list.size
        while (lo < hi) { val mid = (lo + hi) / 2; Drill.compare(lo, hi); if (list[mid] < target) lo = mid + 1 else hi = mid }
        return lo
    }
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        val left = queries[q * 3]; val right = queries[q * 3 + 1]; val value = queries[q * 3 + 2]
        val list = positions[value]
        out[q] = if (list == null) 0 else lowerBound(list, right + 1) - lowerBound(list, left)
        Drill.write(q, out[q])
    }
    return out
}
