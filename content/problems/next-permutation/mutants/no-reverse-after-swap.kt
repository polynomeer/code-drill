// kind: MISSING_EDGE_CASE
// 바꾼 뒤 뒤를 뒤집지 않는다. 다음이 아니라 더 뒤의 순열이 된다.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i < 0) { a.reverse(); return a }
    var j = a.size - 1
    while (a[j] <= a[i]) j -= 1
    val t = a[i]; a[i] = a[j]; a[j] = t
    return a
}
