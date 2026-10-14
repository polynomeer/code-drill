// kind: OFF_BY_ONE
// 자기 자신까지 넣은 최댓값을 쓴다. 오른쪽의 원소들만 본다.
fun greatestOnRight(nums: IntArray): IntArray {
    val a = nums.copyOf(); var best = -1
    for (i in a.indices.reversed()) { best = maxOf(best, a[i]); a[i] = best }
    a[a.size - 1] = -1
    return a
}
