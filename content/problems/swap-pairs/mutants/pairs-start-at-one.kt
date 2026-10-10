// kind: OFF_BY_ONE
// 1과 2, 3과 4 로 묶는다. 묶음은 0과 1 부터다.
fun swapPairs(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = 1
    while (i + 1 < a.size) { val t = a[i]; a[i] = a[i + 1]; a[i + 1] = t; i += 2 }
    return a
}
