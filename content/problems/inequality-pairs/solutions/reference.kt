// 검증용 정답 (§6.1 solutions/). d = nums1 - nums2 를 병합 정렬하며, 오른쪽의 d[j] + diff 이하인 왼쪽 원소를 포인터로 센다.
fun inequalityPairs(nums1: IntArray, nums2: IntArray, diff: Int): Int {
    val n = nums1.size
    val d = IntArray(n) { nums1[it] - nums2[it] }
    val buffer = IntArray(n)
    var count = 0L
    var width = 1
    // 재귀 없이 아래에서부터 — 폭 1, 2, 4, … 의 이웃한 두 덩어리를 센 뒤 합친다
    while (width < n) {
        var low = 0
        while (low < n - width) {
            val mid = low + width
            val high = minOf(low + 2 * width, n)
            var i = low
            for (j in mid until high) {
                while (i < mid && d[i] <= d[j] + diff) i += 1
                count += i - low
            }
            Drill.ret("$low..$high", (count % 1_000_000_007).toInt())
            var a = low; var b = mid; var k = low
            while (a < mid && b < high) buffer[k++] = if (d[a] <= d[b]) d[a++] else d[b++]
            while (a < mid) buffer[k++] = d[a++]
            while (b < high) buffer[k++] = d[b++]
            for (t in low until high) d[t] = buffer[t]
            low += 2 * width
        }
        width *= 2
    }
    return (count % 1_000_000_007).toInt()
}
