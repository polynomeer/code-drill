// kind: WRONG_ALGORITHM
// 0 을 만나면 뒤쪽의 0 아닌 값과 맞바꾼다. 0 이 아닌 값의 순서가 흐트러진다.
fun applyOperations(nums: IntArray): IntArray {
    val a = nums.copyOf()
    for (i in 0 until a.size - 1) if (a[i] == a[i + 1]) { a[i] *= 2; a[i + 1] = 0 }
    var left = 0
    var right = a.size - 1
    while (left < right) {
        if (a[left] != 0) { left += 1; continue }
        if (a[right] == 0) { right -= 1; continue }
        a[left] = a[right]; a[right] = 0
    }
    return a
}
