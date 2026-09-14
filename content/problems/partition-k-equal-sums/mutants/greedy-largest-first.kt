// kind: WRONG_ALGORITHM
// 큰 것부터 지금 가장 덜 찬 묶음에 넣고 끝낸다. 되돌아오지 않는다.
fun canPartition(nums: IntArray, k: Int): Int {
    val total = nums.sum()
    if (total % k != 0) return 0
    val target = total / k
    val buckets = IntArray(k)
    for (v in nums.sortedDescending()) {
        val b = (0 until k).minByOrNull { buckets[it] }!!
        buckets[b] += v
    }
    return if (buckets.all { it == target }) 1 else 0
}
