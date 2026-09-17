// kind: OFF_BY_ONE
// 오른쪽 끝을 빼고 센다. 양 끝 포함이다.
fun rangeFrequency(nums: IntArray, queries: IntArray): IntArray {
    val positions = HashMap<Int, MutableList<Int>>()
    for (i in nums.indices) positions.getOrPut(nums[i]) { ArrayList() }.add(i)
    fun lowerBound(list: List<Int>, target: Int): Int { var lo = 0; var hi = list.size; while (lo < hi) { val mid = (lo + hi) / 2; if (list[mid] < target) lo = mid + 1 else hi = mid }; return lo }
    val out = IntArray(queries.size / 3)
    for (q in out.indices) {
        val list = positions[queries[q * 3 + 2]]
        out[q] = if (list == null) 0 else lowerBound(list, queries[q * 3 + 1]) - lowerBound(list, queries[q * 3])
    }
    return out
}
