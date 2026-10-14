// kind: MISSING_EDGE_CASE
// 처음 들어온 글자를 외톨이가 아니게 된 뒤에도 적는다. 다시 나오면 빠져야 한다.
fun firstUniqueStream(s: String): String {
    val seen = BooleanArray(26); val order = ArrayList<Char>(); val out = StringBuilder()
    for (c in s) { if (!seen[c - 'a']) { seen[c - 'a'] = true; order.add(c) }; out.append(order.first()) }
    return out.toString()
}
