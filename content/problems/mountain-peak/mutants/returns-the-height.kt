// kind: WRONG_BRANCH
// 꼭대기의 값을 낸다. 자리(인덱스)를 낸다.
fun mountainPeak(nums: IntArray): Int {
    var low = 0; var high = nums.size - 1
    while (low < high) { val mid = (low + high) ushr 1; if (nums[mid] < nums[mid + 1]) low = mid + 1 else high = mid }
    return nums[low]
}
