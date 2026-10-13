// kind: OFF_BY_ONE
// 같은 값도 더 작은 것으로 센다. 늘어나는 세 수는 순증가다.
fun increasingTripletsCount(nums: IntArray): Int {
    val n = nums.size
    // 자리 배열을 값으로 병합 정렬하며, 오른쪽 반의 자리가 내려갈 때 이미 내려간 왼쪽 반(더 작은 값)의 수를 더한다
    fun smallerOnTheLeft(values: IntArray): LongArray {
        val counts = LongArray(n)
        var order = IntArray(n) { it }
        var buffer = IntArray(n)
        var width = 1
        while (width < n) {
            var low = 0
            while (low < n) {
                val mid = minOf(low + width, n); val high = minOf(low + 2 * width, n)
                var a = low; var b = mid; var k = low
                while (a < mid || b < high) {
                    // 같은 값이면 오른쪽 반을 먼저 내린다 — 같은 값은 더 작은 것으로 세지 않는다
                    if (b < high && (a >= mid || values[order[b]] < values[order[a]])) {
                        counts[order[b]] += (a - low).toLong()
                        buffer[k++] = order[b++]
                    } else {
                        buffer[k++] = order[a++]
                    }
                }
                low += 2 * width
            }
            val t = order; order = buffer; buffer = t
            width *= 2
        }
        return counts
    }
    val left = smallerOnTheLeft(nums)
    // 오른쪽의 더 큰 수 = 뒤집고 부호를 바꾼 배열에서 왼쪽의 더 작은 수
    val mirrored = IntArray(n) { -nums[n - 1 - it] }
    val rightMirrored = smallerOnTheLeft(mirrored)
    var total = 0L
    for (j in 0 until n) {
        total = (total + left[j] % 1_000_000_007 * (rightMirrored[n - 1 - j] % 1_000_000_007)) % 1_000_000_007
        Drill.write(j, left[j].toInt())
    }
    return total.toInt()
}
