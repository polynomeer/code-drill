// kind: WRONG_ALGORITHM
// 해시 집합에 넣고 꺼낸다. 순서가 값의 해시 순서다.
fun removeDuplicates(nums: IntArray): IntArray = nums.toHashSet().toIntArray()
