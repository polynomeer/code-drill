// kind: MISSING_EDGE_CASE
// 마지막 원소를 그대로 둔다. 오른쪽에 아무것도 없으면 -1 이다.
fun greatestOnRight(nums: IntArray): IntArray {
    val a = nums.copyOf(); var best = a[a.size - 1]
    for (i in a.size - 2 downTo 0) { val original = a[i]; a[i] = best; if (original > best) best = original }
    return a
}
