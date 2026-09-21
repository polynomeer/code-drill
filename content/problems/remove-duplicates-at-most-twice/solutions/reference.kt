// 검증용 정답 (§6.1 solutions/). 두 칸 앞과 비교하며 당겨 쓴다.
fun removeDuplicatesAtMostTwice(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var write = 0
    for (read in a.indices) {
        Drill.compare(read, write)
        if (write < 2 || a[write - 2] != a[read]) { a[write] = a[read]; Drill.write(write, a[read]); write += 1 }
    }
    return a.copyOf(write)
}
