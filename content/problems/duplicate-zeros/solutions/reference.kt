// 검증용 정답 (§6.1 solutions/). 살아남는 원소를 센 뒤, 뒤에서 앞으로 제자리에 쓴다.
fun duplicateZeros(arr: IntArray): IntArray {
    val n = arr.size
    var zeros = 0
    var last = 0
    var length = 0
    // 몇 번째 원소까지 살아남나
    while (last < n) {
        val need = if (arr[last] == 0) 2 else 1
        if (length + need > n) break
        length += need
        if (arr[last] == 0) zeros += 1
        last += 1
    }
    var write = n - 1
    // 마지막으로 살아남은 0 이 한 칸만 남았다
    if (last < n && arr[last] == 0 && length == n - 1) {
        arr[write] = 0
        Drill.write(write, 0)
        write -= 1
    }
    for (read in last - 1 downTo 0) {
        arr[write] = arr[read]
        Drill.write(write, arr[read])
        write -= 1
        if (arr[read] == 0) {
            arr[write] = 0
            write -= 1
        }
    }
    return arr
}
