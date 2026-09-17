// kind: MISSING_EDGE_CASE
// 내림차순(마지막)이면 그대로 돌려준다. 처음(오름차순)으로 돌아가야 한다.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i < 0) return a
    var j = a.size - 1
    while (a[j] <= a[i]) j -= 1
    val t = a[i]; a[i] = a[j]; a[j] = t
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val u = a[lo]; a[lo] = a[hi]; a[hi] = u; lo += 1; hi -= 1 }
    return a
}
