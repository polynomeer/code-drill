// kind: OFF_BY_ONE
// nums[lo] < nums[mid] 일 때만 왼쪽이 정렬됐다고 본다. 원소가 하나 남아 lo == mid 면 왼쪽을 놓친다.
fun searchRotated(nums: IntArray, queries: IntArray): IntArray = IntArray(queries.size) { q ->
    val t = queries[q]
    var lo = 0; var hi = nums.size - 1; var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (nums[mid] == t) { found = mid; break }
        if (nums[lo] < nums[mid]) { if (nums[lo] <= t && t < nums[mid]) hi = mid - 1 else lo = mid + 1 }
        else { if (nums[mid] < t && t <= nums[hi]) lo = mid + 1 else hi = mid - 1 }
    }
    found
}
