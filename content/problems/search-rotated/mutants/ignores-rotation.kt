// kind: WRONG_ALGORITHM
// 정렬된 배열처럼 이분 탐색한다. 잘라 붙인 자리 너머의 값을 찾지 못한다.
fun searchRotated(nums: IntArray, queries: IntArray): IntArray = IntArray(queries.size) { q ->
    val t = queries[q]
    var lo = 0; var hi = nums.size - 1; var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (nums[mid] == t) { found = mid; break }
        if (nums[mid] < t) lo = mid + 1 else hi = mid - 1
    }
    found
}
