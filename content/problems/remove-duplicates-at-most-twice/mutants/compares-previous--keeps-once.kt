// kind: OFF_BY_ONE
// 바로 앞과 비교한다. 한 번만 남는다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) if (write < 1 || a[write - 1] != a[read]) { a[write] = a[read]; write += 1 }
    return a.copyOf(write)
}
