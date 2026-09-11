// 검증용 정답 (§6.1 solutions/). 해시맵으로 세고, (빈도 내림, 값 오름)으로 정렬해 k 개.
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (i in nums.indices) {
        Drill.visit(i, nums[i])
        counts[nums[i]] = (counts[nums[i]] ?: 0) + 1
    }
    val ordered = counts.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
    val out = IntArray(k)
    for (i in 0 until k) {
        out[i] = ordered[i].key
        Drill.push(out[i])
    }
    return out
}
