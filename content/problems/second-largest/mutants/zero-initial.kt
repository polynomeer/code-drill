// kind: MISSING_EDGE_CASE
// 둘째의 초기값을 0 으로 둔다. 최댓값이 맨 앞이고 나머지가 음수면 아무것도 둘째가 못 된다.
fun secondLargest(nums: IntArray): Int {
    var first = nums[0]; var second = 0
    for (v in nums) {
        if (v > first) { second = first; first = v }
        else if (v < first && v > second) second = v
    }
    return if (second != first && nums.contains(second)) second else -1
}
