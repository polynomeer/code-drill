// kind: PERFORMANCE
// 자리마다 뒤를 훑는다. O(n²).
fun countSmallerAfter(nums: IntArray): IntArray {
    val n = nums.size
    val counts = IntArray(n)
    for (i in 0 until n) {
        var c = 0
        for (j in i + 1 until n) { Drill.compare(i, j); if (nums[j] < nums[i]) c += 1 }
        counts[i] = c
    }
    return counts
}
