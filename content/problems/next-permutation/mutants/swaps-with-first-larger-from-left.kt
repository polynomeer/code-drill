// kind: WRONG_BRANCH
// i 뒤에서 왼쪽부터 처음으로 큰 원소와 바꾼다. 가장 작은 큰 원소가 아니다.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) i -= 1
    if (i >= 0) {
        var j = i + 1
        while (a[j] <= a[i]) j += 1
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; lo += 1; hi -= 1 }
    return a
}
