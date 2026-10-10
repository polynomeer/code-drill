// kind: WRONG_ALGORITHM
// 큰 봉지부터 지금 가장 적게 받은 아이에게 준다. 탐욕은 최적을 보장하지 않는다.
fun distributeCookies(bags: IntArray, k: Int): Int {
    val kids = IntArray(k)
    for (b in bags.sortedDescending()) { val i = kids.indices.minByOrNull { kids[it] }!!; kids[i] += b }
    return kids.maxOrNull()!!
}
