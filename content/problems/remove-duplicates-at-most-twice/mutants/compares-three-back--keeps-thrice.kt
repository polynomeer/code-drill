// kind: OFF_BY_ONE
// 세 칸 앞과 비교한다. 세 번이 남는다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) if (write < 3 || a[write - 3] != a[read]) { a[write] = a[read]; write += 1 }
    return a.copyOf(write)
}
