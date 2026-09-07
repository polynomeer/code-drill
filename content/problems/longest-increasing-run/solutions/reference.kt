// 검증용 정답 (§6.1 solutions/). 길이별 최소 끝값 + 이분 탐색.
//
// tails[i] 는 "길이 i+1 인 증가 수열이 가질 수 있는 가장 작은 끝값"이다. 끝값이
// 작을수록 뒤에 이어 붙일 여지가 크므로, 각 길이에 대해 최소값만 들고 있으면 된다.
//
// 왼쪽 경계를 찾는 이분 탐색이라 같은 값은 기존 자리를 덮어쓴다 — 엄격히 증가해야
// 하므로 같은 값으로 길이를 늘리지 않는다.
fun longestIncreasing(nums: IntArray): Int {
    val tails = IntArray(nums.size)
    var length = 0

    for (index in nums.indices) {
        val value = nums[index]
        Drill.visit(index, value)

        var low = 0
        var high = length
        while (low < high) {
            val mid = low + (high - low) / 2
            if (tails[mid] < value) low = mid + 1 else high = mid
        }
        tails[low] = value
        Drill.write(low, value)
        if (low == length) length += 1
    }
    return length
}
