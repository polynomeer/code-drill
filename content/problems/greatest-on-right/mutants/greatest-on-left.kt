// kind: WRONG_ALGORITHM
// 왼쪽에서 오른쪽으로 가며 왼쪽의 최댓값을 쓴다. 오른쪽의 최댓값이다.
fun greatestOnRight(nums: IntArray): IntArray {
    val a = nums.copyOf(); var best = -1
    for (i in a.indices) { val original = a[i]; a[i] = best; if (original > best) best = original }
    return a
}
