// kind: WRONG_BRANCH
// 만든 순서대로 내놓는다. 답은 번호 오름차순이다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val users = Array(n) { ArrayList<Int>() }
    val missing = IntArray(n)
    for (i in recipes.indices step 2) { users[recipes[i]].add(recipes[i + 1]); missing[recipes[i + 1]] += 1 }
    val have = BooleanArray(n)
    val queue = java.util.ArrayDeque<Int>()
    for (s in supplies) { have[s] = true; queue.add(s) }
    val made = ArrayList<Int>()
    while (queue.isNotEmpty()) {
        val x = queue.poll()
        for (p in users[x]) { missing[p] -= 1; if (missing[p] == 0 && !have[p]) { have[p] = true; made.add(p); queue.add(p) } }
    }
    return made.toIntArray()
}
