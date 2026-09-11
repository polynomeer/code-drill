// kind: MISSING_EDGE_CASE
// 빈도만으로 정렬한다. 빈도가 같으면 해시맵 순서가 답이 된다.
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedByDescending { it.value }
    return IntArray(k) { ordered[it].key }
}
