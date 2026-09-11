// kind: WRONG_BRANCH
// 빈도가 같을 때 값을 내림차순으로 둔다. 둘째 정렬 기준을 반대로 읽었다.
fun topK(nums: IntArray, k: Int): IntArray {
    val counts = HashMap<Int, Int>()
    for (v in nums) counts[v] = (counts[v] ?: 0) + 1
    val ordered = counts.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
    return IntArray(k) { ordered[it].key }
}
