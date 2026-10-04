// kind: WRONG_BRANCH
// 재료가 없는 물건을 처음부터 가진 것으로 본다. 재료가 없으면 가진 것만 쓸 수 있다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    val missing = IntArray(n)
    for (i in recipes.indices step 2) { users[recipes[i]].add(recipes[i + 1]); missing[recipes[i + 1]] += 1 }
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    for (s in supplies) supplied[s] = true
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (supplied[v] || missing[v] == 0) { have[v] = true; queue.add(v) }
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) { missing[p] -= 1; if (missing[p] == 0 && !have[p]) { have[p] = true; queue.add(p) } }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
