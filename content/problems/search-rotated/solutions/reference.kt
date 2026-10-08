// 검증용 정답 (§6.1 solutions/). 가운데에서 정렬된 쪽을 골라 그 범위로 판단한다.
fun searchRotated(nums: IntArray, queries: IntArray): IntArray = IntArray(queries.size) { q ->
    val t = queries[q]
    var lo = 0
    var hi = nums.size - 1
    var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        Drill.pointer("lo", lo)
        Drill.pointer("hi", hi)
        if (nums[mid] == t) { found = mid; break }
        if (nums[lo] <= nums[mid]) {
            if (nums[lo] <= t && t < nums[mid]) hi = mid - 1 else lo = mid + 1
        } else {
            if (nums[mid] < t && t <= nums[hi]) lo = mid + 1 else hi = mid - 1
        }
    }
    found
}
