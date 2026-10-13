// 검증용 정답 (§6.1 solutions/). 오른쪽 이웃이 더 크면 꼭대기는 오른쪽, 아니면 mid 이하 — mid 를 버리지 않는다.
fun mountainPeak(nums: IntArray): Int {
    var low = 0
    var high = nums.size - 1
    while (low < high) {
        val mid = (low + high) ushr 1
        if (nums[mid] < nums[mid + 1]) low = mid + 1 else high = mid
        Drill.pointer("low", low); Drill.pointer("high", high)
    }
    return low
}
