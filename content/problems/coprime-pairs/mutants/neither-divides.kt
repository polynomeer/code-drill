// kind: WRONG_ALGORITHM
// 어느 쪽도 다른 쪽을 나누지 않으면 서로소로 본다. 6 과 10 은 서로를 나누지 않지만 2 를 함께 가진다.
fun coprimePairs(nums: IntArray): Int {
    var count = 0
    for (i in nums.indices) for (j in i + 1 until nums.size) if (nums[i] % nums[j] != 0 && nums[j] % nums[i] != 0) count += 1
    return count
}
