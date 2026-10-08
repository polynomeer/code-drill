// kind: MISSING_EDGE_CASE
// 없는 값에 끝난 자리를 돌려준다. 없으면 -1 이다.
fun searchRotated(nums: IntArray, queries: IntArray): IntArray = IntArray(queries.size) { q ->
    val t = queries[q]
    var lo = 0; var hi = nums.size - 1; var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (nums[mid] == t) { found = mid; break }
        if (nums[lo] <= nums[mid]) { if (nums[lo] <= t && t < nums[mid]) hi = mid - 1 else lo = mid + 1 }
        else { if (nums[mid] < t && t <= nums[hi]) lo = mid + 1 else hi = mid - 1 }
    }
    if (found == -1) minOf(lo, nums.size - 1) else found
}
