// 검증용 정답 (§6.1 solutions/). 끝자리부터 올림을 넘긴다.
fun plusOne(digits: IntArray): IntArray {
    val out = digits.copyOf()
    for (i in out.indices.reversed()) {
        Drill.visit(i, out[i])
        if (out[i] < 9) { out[i] += 1; Drill.write(i, out[i]); return out }
        out[i] = 0
        Drill.write(i, 0)
    }
    val grown = IntArray(out.size + 1)
    grown[0] = 1
    return grown
}
