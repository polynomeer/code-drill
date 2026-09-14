// kind: WRONG_BRANCH
// 개수가 0 이 돼도 후보를 바꾸지 않는다. 첫 원소가 곧 답이 된다.
fun majority(nums: IntArray): Int {
    val candidate = nums[0]
    var count = 0
    for (v in nums) count += if (v == candidate) 1 else -1
    return candidate
}
