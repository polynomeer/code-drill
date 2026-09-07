// 검증용 정답 (§6.1 solutions/). 병합 정렬로 세기.
//
// 병합할 때 오른쪽 값이 먼저 나오면, 왼쪽에 남아 있는 원소는 전부 그 값보다 크다.
// 그래서 한 번에 "남은 개수"만큼 역순 쌍을 더할 수 있고, 이것이 O(n^2) 를 O(n log n)
// 으로 줄이는 지점이다.
fun countInversions(nums: IntArray): Int {
    val buffer = IntArray(nums.size)
    val values = nums.copyOf()
    var count = 0

    fun sort(from: Int, to: Int) {
        if (to - from <= 1) return
        val mid = (from + to) / 2
        Drill.call("sort")
        sort(from, mid)
        sort(mid, to)

        var left = from
        var right = mid
        var write = from

        while (left < mid && right < to) {
            Drill.compare(left, right)
            if (values[left] <= values[right]) {
                buffer[write] = values[left]
                left += 1
            } else {
                buffer[write] = values[right]
                right += 1
                // 왼쪽에 남은 것은 전부 이 값보다 크다.
                count += mid - left
            }
            Drill.write(write, buffer[write])
            write += 1
        }
        while (left < mid) { buffer[write] = values[left]; left += 1; write += 1 }
        while (right < to) { buffer[write] = values[right]; right += 1; write += 1 }
        for (i in from until to) values[i] = buffer[i]
        Drill.ret("sort", count)
    }

    sort(0, values.size)
    return count
}
