// kind: PERFORMANCE
// 더 만들 것이 없을 때까지 모든 물건을 처음부터 다시 훑는다. 한 바퀴에 하나씩만 늘면 물건 수만큼 돈다.
fun craftableItems(n: Int, recipes: IntArray, supplies: IntArray): IntArray {
    val ingredients = Array(n) { ArrayList<Int>() }
    for (i in recipes.indices step 2) ingredients[recipes[i + 1]].add(recipes[i])
    val have = BooleanArray(n)
    val supplied = BooleanArray(n)
    for (s in supplies) { have[s] = true; supplied[s] = true }
    var changed = true
    while (changed) {
        changed = false
        for (p in 0 until n) {
            if (have[p] || ingredients[p].isEmpty()) continue
            var ready = true
            for (a in ingredients[p]) { Drill.compare(p, a); if (!have[a]) { ready = false; break } }
            if (ready) { have[p] = true; changed = true }
        }
    }
    return (0 until n).filter { have[it] && !supplied[it] }.toIntArray()
}
