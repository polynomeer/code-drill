// kind: PERFORMANCE
// 왼쪽 끝마다 오른쪽 끝을 배열 끝에서부터 찾아 내려온다. 짝이 없는 자리마다 나머지를 전부 본다.
fun maxWidthRamp(nums: IntArray): Int {
    var best = 0
    for (i in nums.indices) {
        var j = nums.size - 1
        while (j > i) {
            if (nums[i] <= nums[j]) { best = maxOf(best, j - i); break }
            j -= 1
        }
    }
    return best
}
