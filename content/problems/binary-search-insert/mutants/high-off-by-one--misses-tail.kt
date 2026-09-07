// kind: OFF_BY_ONE
// high 를 size - 1 에서 시작해 모든 원소보다 큰 값의 자리를 만들지 못한다.
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size - 1
    while (low < high) {
        val mid = low + (high - low) / 2
        if (nums[mid] < target) low = mid + 1 else high = mid
    }
    return low
}
