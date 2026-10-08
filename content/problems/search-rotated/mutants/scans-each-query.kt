// kind: PERFORMANCE
// 질문마다 배열을 앞에서부터 훑는다. 없는 값은 끝까지 훑어 질문 수 × 길이다.
fun searchRotated(nums: IntArray, queries: IntArray): IntArray = IntArray(queries.size) { q ->
    var found = -1
    for (i in nums.indices) { Drill.compare(i, q); if (nums[i] == queries[q]) { found = i; break } }
    found
}
