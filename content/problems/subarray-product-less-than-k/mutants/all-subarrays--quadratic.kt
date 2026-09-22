// kind: PERFORMANCE
// 모든 구간의 곱을 본다. O(n²) — 1 이 많으면 끊기지도 않는다.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in nums.indices) {
        var product = 1L
        for (j in i until nums.size) {
            Drill.compare(i, j)
            product *= nums[j]
            if (product >= k) break
            total += 1
        }
    }
    return total
}
