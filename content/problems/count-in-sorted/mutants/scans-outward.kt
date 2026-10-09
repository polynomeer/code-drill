// kind: PERFORMANCE
// 이분 탐색으로 하나를 찾고 양옆으로 센다. 같은 값이 많으면 질문마다 그 개수만큼 걷는다.
fun countInSorted(nums: IntArray, targets: IntArray): IntArray = IntArray(targets.size) {
    val t = targets[it]
    val at = java.util.Arrays.binarySearch(nums, t)
    if (at < 0) 0 else {
        var left = at; var right = at
        while (left > 0 && nums[left - 1] == t) left -= 1
        while (right < nums.size - 1 && nums[right + 1] == t) right += 1
        right - left + 1
    }
}
