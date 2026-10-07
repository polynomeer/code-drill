// kind: WRONG_ALGORITHM
// 지운 자리를 끝 원소로 메운다. 남은 원소의 순서가 바뀐다.
fun removeElement(nums: IntArray, value: Int): IntArray {
    var i = 0
    var n = nums.size
    while (i < n) {
        if (nums[i] == value) { nums[i] = nums[n - 1]; n -= 1 } else i += 1
    }
    return nums.copyOf(n)
}
