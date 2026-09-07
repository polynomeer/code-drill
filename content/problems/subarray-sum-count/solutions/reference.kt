// 검증용 정답 (§6.1 solutions/). 접두사 합 + 빈도 맵.
//
// 지금까지의 합이 total 일 때, 합이 k 인 구간은 "이전에 total - k 였던 지점"의 수만큼
// 있다. 그 수를 세어 두면 한 번 훑기로 끝난다.
//
// 빈 접두사를 1 로 시작하는 것이 중요하다. 배열 맨 앞에서 시작하는 구간은 이전 지점이
// 없으므로, 그 자리를 미리 만들어 두지 않으면 세어지지 않는다.
fun countSubarrays(nums: IntArray, k: Int): Int {
    val seen = HashMap<Int, Int>()
    seen[0] = 1

    var total = 0
    var count = 0

    for (index in nums.indices) {
        Drill.visit(index, nums[index])
        total += nums[index]
        Drill.write(index, total)

        val hits = seen[total - k] ?: 0
        if (hits > 0) {
            count += hits
            Drill.match(index, index)
        }
        seen[total] = (seen[total] ?: 0) + 1
    }
    return count
}
