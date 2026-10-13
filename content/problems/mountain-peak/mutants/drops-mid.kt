// kind: OFF_BY_ONE
// 내리막이면 high 를 mid - 1 로 옮긴다. mid 가 꼭대기일 수 있다.
fun mountainPeak(nums: IntArray): Int {
    var low = 0; var high = nums.size - 1
    while (low < high) { val mid = (low + high) ushr 1; if (nums[mid] < nums[mid + 1]) low = mid + 1 else high = mid - 1 }
    return low
}
