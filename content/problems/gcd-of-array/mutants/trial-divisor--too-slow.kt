// kind: PERFORMANCE
// 가장 작은 값부터 하나씩 나눠 본다. 값이 크면 무너진다.
fun gcdOfArray(nums: IntArray): Int {
    val values = nums.map { if (it < 0) -it else it }
    val smallest = values.filter { it > 0 }.minOrNull() ?: return 0
    for (candidate in smallest downTo 1) {
        if (values.all { it % candidate == 0 }) return candidate
    }
    return 0
}
