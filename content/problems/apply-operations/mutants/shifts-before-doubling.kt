// kind: WRONG_ALGORITHM
// 0 을 먼저 뒤로 밀고 두 배로 만든다. 그러면 0 을 사이에 둔 값이 이웃이 되고, 새로 생긴 0 은 밀리지 않는다.
fun applyOperations(nums: IntArray): IntArray {
    val kept = nums.filter { it != 0 }
    val a = IntArray(nums.size) { if (it < kept.size) kept[it] else 0 }
    for (i in 0 until a.size - 1) if (a[i] == a[i + 1]) { a[i] *= 2; a[i + 1] = 0 }
    return a
}
