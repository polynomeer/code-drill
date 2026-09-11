// 검증용 정답 (§6.1 solutions/). 원소마다 둘로 갈라지는 완전탐색. 2^20 은 충분히 작다.
fun subsetCount(nums: IntArray, target: Int): Int {
    fun go(i: Int, remaining: Int): Int {
        Drill.call("i=$i")
        val found = if (i == nums.size) {
            if (remaining == 0) 1 else 0
        } else {
            go(i + 1, remaining - nums[i]) + go(i + 1, remaining)
        }
        Drill.ret("i=$i", found)
        return found
    }
    return go(0, target)
}
