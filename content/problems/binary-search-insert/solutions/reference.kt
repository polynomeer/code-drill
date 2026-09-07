// 검증용 정답 (§6.1 solutions/). 왼쪽 경계를 찾는 이분 탐색.
//
// 찾자마자 반환하지 않는다. 같은 값이 여러 개일 때 "가장 왼쪽"을 답으로 하려면
// 구간을 계속 좁혀 low 와 high 가 만나는 자리를 봐야 한다.
//
// high 를 nums.size 에서 시작하는 것도 의도다. 모든 원소보다 큰 값의 답이 곧
// nums.size 이므로, size - 1 에서 시작하면 그 답을 만들 수 없다.
fun insertPosition(nums: IntArray, target: Int): Int {
    var low = 0
    var high = nums.size

    while (low < high) {
        val mid = low + (high - low) / 2
        Drill.pointer("low", low)
        Drill.pointer("high", high)
        Drill.visit(mid, nums[mid])

        if (nums[mid] < target) low = mid + 1 else high = mid
    }
    return low
}
