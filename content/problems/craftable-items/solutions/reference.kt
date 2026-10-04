// 검증용 정답 (§6.1 solutions/). 가진 물건에서 출발하는 위상 정렬 — 모자란 재료의 수를 줄여 간다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val m = recipes.size / 2
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val missing = IntArray(n)
    for (e in 0 until m) {
        val a = recipes[2 * e]; val b = recipes[2 * e + 1]
        to[e] = b; next[e] = head[a]; head[a] = e
        missing[b] += 1
    }
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    val queue = IntArray(n)
    var tail = 0
    for (s in supplies) { have[s] = true; supplied[s] = true; queue[tail] = s; tail += 1 }
    var front = 0
    while (front < tail) {
        val x = queue[front]
        front += 1
        Drill.dequeue(x)
        var e = head[x]
        while (e != -1) {
            val p = to[e]
            missing[p] -= 1
            Drill.write(p, missing[p])
            if (missing[p] == 0 && !have[p]) { have[p] = true; queue[tail] = p; tail += 1 }
            e = next[e]
        }
    }
    var count = 0
    for (v in 0 until n) if (have[v] && !supplied[v]) count += 1
    val out = IntArray(count)
    var k = 0
    for (v in 0 until n) if (have[v] && !supplied[v]) { out[k] = v; k += 1 }
    return out
}
