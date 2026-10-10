// kind: WRONG_BRANCH
// 길이가 홀수면 마지막 원소를 맨 앞과 바꾼다. 짝이 없으면 그대로다.
fun swapPairs(nums: IntArray): IntArray {
    val a = nums.copyOf()
    var i = 0
    while (i + 1 < a.size) { val t = a[i]; a[i] = a[i + 1]; a[i + 1] = t; i += 2 }
    if (a.size % 2 == 1 && a.size > 1) { val t = a[0]; a[0] = a[a.size - 1]; a[a.size - 1] = t }
    return a
}
