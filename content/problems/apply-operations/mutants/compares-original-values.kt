// kind: WRONG_BRANCH
// 원래 배열로 비교한다. 앞 단계가 0 으로 만든 칸을 원래 값으로 보고 다시 두 배로 만든다.
fun applyOperations(nums: IntArray): IntArray {
    val a = nums.copyOf()
    for (i in 0 until a.size - 1) if (nums[i] == nums[i + 1]) { a[i] *= 2; a[i + 1] = 0 }
    val kept = a.filter { it != 0 }
    return IntArray(a.size) { if (it < kept.size) kept[it] else 0 }
}
