// kind: WRONG_BRANCH
// 처음 나온 하나만 지운다. 같은 값은 모두 지워야 한다.
fun removeElement(nums: IntArray, value: Int): IntArray {
    val at = nums.indexOf(value)
    if (at < 0) return nums
    return nums.filterIndexed { i, _ -> i != at }.toIntArray()
}
