// kind: MISSING_EDGE_CASE
// 중복을 없애고 세어 같은 값이 여러 개면 틀린다.
fun kthLargest(nums: IntArray, k: Int): Int {
    val distinct = nums.distinct().sortedDescending()
    return distinct[minOf(k, distinct.size) - 1]
}
