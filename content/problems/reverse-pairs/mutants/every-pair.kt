// kind: PERFORMANCE
// 모든 쌍을 견준다. n 이 6 만 5 천이면 21 억 번이다.
fun reversePairs(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) for (j in i + 1 until nums.size) {
        Drill.compare(i, j)
        if (nums[i].toLong() > 2L * nums[j]) count += 1
    }
    return count
}
