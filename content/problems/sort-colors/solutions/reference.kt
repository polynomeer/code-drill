// 검증용 정답 (§6.1 solutions/). 네덜란드 국기 — 한 번 훑는다.
fun sortColors(nums: IntArray): IntArray {
    val out = nums.copyOf()
    var low = 0
    var mid = 0
    var high = out.size - 1
    while (mid <= high) {
        when (out[mid]) {
            0 -> { val t = out[low]; out[low] = out[mid]; out[mid] = t; Drill.swap(low, mid); low += 1; mid += 1 }
            2 -> { val t = out[high]; out[high] = out[mid]; out[mid] = t; Drill.swap(mid, high); high -= 1 }
            else -> mid += 1
        }
        Drill.pointer("low", low)
        Drill.pointer("high", high)
    }
    return out
}
