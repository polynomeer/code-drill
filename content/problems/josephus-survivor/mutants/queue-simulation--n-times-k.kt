// kind: PERFORMANCE
// 큐로 k-1 명씩 뒤로 보내며 흉내 낸다. O(n·k).
fun survivor(n: Int, k: Int): Int {
    val queue = ArrayDeque<Int>()
    for (i in 1..n) queue.addLast(i)
    while (queue.size > 1) {
        repeat(k - 1) { val p = queue.removeFirst(); Drill.enqueue(p); queue.addLast(p) }
        Drill.dequeue(queue.removeFirst())
    }
    return queue.first()
}
