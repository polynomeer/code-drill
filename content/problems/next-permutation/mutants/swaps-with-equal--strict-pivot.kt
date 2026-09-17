// kind: OFF_BY_ONE
// 꺾이는 자리를 a[i] > a[i+1] 로 찾고 바꿀 상대를 a[j] >= a[i] 로 찾는다. 중복에서 틀린다.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] > a[i + 1]) i -= 1
    if (i >= 0) {
        var j = a.size - 1
        while (a[j] < a[i]) j -= 1
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; lo += 1; hi -= 1 }
    return a
}
