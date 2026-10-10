// kind: MISSING_EDGE_CASE
// 쌍의 수를 Int 로 센다. 쌍은 1 250 억까지 가서 나머지를 취하기 전에 넘친다.
fun inequalityPairs(nums1: IntArray, nums2: IntArray, diff: Int): Int {
    val n = nums1.size
    val d = IntArray(n) { nums1[it] - nums2[it] }
    val buffer = IntArray(n); var count = 0; var width = 1
    while (width < n) {
        var low = 0
        while (low < n - width) {
            val mid = low + width; val high = minOf(low + 2 * width, n)
            var i = low
            for (j in mid until high) { while (i < mid && d[i] <= d[j] + diff) i += 1; count += i - low }
            var a = low; var b = mid; var k = low
            while (a < mid && b < high) buffer[k++] = if (d[a] <= d[b]) d[a++] else d[b++]
            while (a < mid) buffer[k++] = d[a++]
            while (b < high) buffer[k++] = d[b++]
            for (t in low until high) d[t] = buffer[t]
            low += 2 * width
        }
        width *= 2
    }
    return count % 1_000_000_007
}
