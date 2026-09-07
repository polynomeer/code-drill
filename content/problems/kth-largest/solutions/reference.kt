// 검증용 정답 (§6.1 solutions/). 정렬 후 인덱스.
//
// O(n log n) 이다. 선택 알고리즘으로 O(n) 까지 줄일 수 있지만, 이 문제의 성능 그룹은
// O(n^2) 를 걸러 내는 것이 목적이라 정렬이면 충분하다.
fun kthLargest(nums: IntArray, k: Int): Int {
    val sorted = nums.sortedDescending()
    for (index in 0 until minOf(k, sorted.size)) {
        Drill.visit(index, sorted[index])
    }
    Drill.write(0, sorted[k - 1])
    return sorted[k - 1]
}
