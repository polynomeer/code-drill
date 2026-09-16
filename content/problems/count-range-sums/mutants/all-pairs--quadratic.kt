// kind: PERFORMANCE
// 누적합의 모든 쌍을 본다. O(n²).
fun countRangeSums(nums: IntArray, lower: Int, upper: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    var total = 0
    for (i in 0..n) {
        for (j in i + 1..n) {
            Drill.compare(i, j)
            val sum = prefix[j] - prefix[i]
            if (sum >= lower && sum <= upper) total += 1
        }
    }
    return total
}
