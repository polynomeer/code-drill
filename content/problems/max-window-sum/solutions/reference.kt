// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우.
//
// 창을 옮길 때 전체를 다시 더하지 않는다. 들어온 값을 더하고 나간 값을 빼면
// O(n) 이며, 다시 더하는 구현은 O(n*k) 라 성능 그룹에서 갈린다.
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) {
        Drill.visit(i, nums[i])
        total += nums[i]
    }

    var best = total
    Drill.write(0, total)

    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        Drill.pointer("right", i)
        Drill.write(0, total)
        if (total > best) best = total
    }
    return best
}
