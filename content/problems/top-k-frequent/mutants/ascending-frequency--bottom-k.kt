// kind: WRONG_BRANCH
// 빈도 오름차순으로 정렬한다. 가장 드문 값을 낸다.
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedWith(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
    return IntArray(k) { ordered[it].key }
}
