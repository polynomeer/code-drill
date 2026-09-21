// kind: WRONG_BRANCH
// 값이 바뀌어도 개수를 0 으로 돌리지 않는다. 처음 둘만 남는다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    var count = 0
    for (read in a.indices) {
        count += 1
        if (count <= 2) { a[write] = a[read]; write += 1 }
    }
    return a.copyOf(write)
}
