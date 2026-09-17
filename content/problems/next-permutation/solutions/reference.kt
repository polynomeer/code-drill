// 검증용 정답 (§6.1 solutions/). 꺾이는 자리, 뒤에서 처음 큰 것과 교환, 뒤집기.
fun nextPermutation(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = a.size - 2
    while (i >= 0 && a[i] >= a[i + 1]) { Drill.compare(i, i + 1); i -= 1 }
    if (i >= 0) {
        var j = a.size - 1
        while (a[j] <= a[i]) { Drill.compare(i, j); j -= 1 }
        val t = a[i]; a[i] = a[j]; a[j] = t
        Drill.swap(i, j)
    }
    var lo = i + 1; var hi = a.size - 1
    while (lo < hi) { val t = a[lo]; a[lo] = a[hi]; a[hi] = t; Drill.swap(lo, hi); lo += 1; hi -= 1 }
    return a
}
