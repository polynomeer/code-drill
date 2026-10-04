// kind: WRONG_ALGORITHM
// 재료 하나만 갖춰도 만들 수 있다고 본다. 재료를 모두 갖춰야 한다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    for (i in recipes.indices step 2) users[recipes[i]].add(recipes[i + 1])
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    val queue = java.util.ArrayDeque<Int>()
    for (s in supplies) { have[s] = true; supplied[s] = true; queue.add(s) }
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) if (!have[p]) { have[p] = true; queue.add(p) }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
