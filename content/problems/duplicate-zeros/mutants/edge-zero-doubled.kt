// kind: OFF_BY_ONE
// 마지막으로 살아남은 0 이 한 칸만 남아도 두 칸을 쓴다. 한 칸을 넘어 앞의 원소를 밀어낸다.
fun duplicateZeros(arr: IntArray): IntArray {
    val n = arr.size
    var last = 0
    var length = 0
    while (last < n && length < n) { length += if (arr[last] == 0) 2 else 1; last += 1 }
    var write = n - 1
    for (read in last - 1 downTo 0) {
        if (write >= 0) arr[write] = arr[read]
        write -= 1
        if (arr[read] == 0 && write >= 0) { arr[write] = 0; write -= 1 }
    }
    return arr
}
