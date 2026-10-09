// kind: OFF_BY_ONE
// 마지막 쌍(n-2, n-1)을 보지 않는다.
fun applyOperations(nums: IntArray): IntArray {
    val a = nums.copyOf()
    for (i in 0 until a.size - 2) if (a[i] == a[i + 1]) { a[i] *= 2; a[i + 1] = 0 }
    val kept = a.filter { it != 0 }
    return IntArray(a.size) { if (it < kept.size) kept[it] else 0 }
}
