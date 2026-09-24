// 검증용 정답 (§6.1 solutions/). 오를 수 있는 데까지 오르고, 거기서 끝까지 내려가는지 본다.
fun isMountain(nums: IntArray): Int {
    val n = nums.size
    var i = 0
    while (i + 1 < n && nums[i] < nums[i + 1]) { Drill.compare(i, i + 1); i += 1 }
    Drill.pointer("peak", i)
    if (i == 0 || i == n - 1) return 0
    while (i + 1 < n && nums[i] > nums[i + 1]) { Drill.compare(i, i + 1); i += 1 }
    return if (i == n - 1) 1 else 0
}
