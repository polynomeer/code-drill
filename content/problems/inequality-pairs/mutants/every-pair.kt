// kind: PERFORMANCE
// 모든 쌍을 본다. 맞지만, 50 만이면 1 250 억 번의 비교다.
fun inequalityPairs(nums1: IntArray, nums2: IntArray, diff: Int): Int {
    val n = nums1.size
    val d = IntArray(n) { nums1[it] - nums2[it] }
    var count = 0L
    for (i in 0 until n) for (j in i + 1 until n) if (d[i] <= d[j] + diff) count += 1
    return (count % 1_000_000_007).toInt()
}
