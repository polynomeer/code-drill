// kind: WRONG_BRANCH
// 찾자마자 반환해 같은 값이 여러 개일 때 가운데를 돌려준다.
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size - 1
    while (low <= high) {
        val mid = low + (high - low) / 2
        if (nums[mid] == target) return mid
        if (nums[mid] < target) low = mid + 1 else high = mid - 1
    }
    return low
}
