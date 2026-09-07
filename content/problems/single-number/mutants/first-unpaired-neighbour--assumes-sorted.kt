// kind: WRONG_BRANCH
// 이웃한 두 개씩 짝지어 본다. 정렬돼 있다고 가정한 셈이라 섞여 있으면 틀린다.
fun singleNumber(nums: IntArray): Int {
    var index = 0
    while (index + 1 < nums.size) {
        if (nums[index] != nums[index + 1]) return nums[index]
        index += 2
    }
    return nums[nums.size - 1]
}
