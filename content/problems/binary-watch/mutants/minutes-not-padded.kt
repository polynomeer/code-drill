// kind: MISSING_EDGE_CASE
// 분을 두 자리로 맞추지 않는다. 1:5 가 아니라 1:05 다.
fun binaryWatch(lit: Int): Array<String> {
    val out = ArrayList<String>()
    for (hour in 0 until 12) for (minute in 0 until 60) if (Integer.bitCount(hour) + Integer.bitCount(minute) == lit) out.add("$hour:$minute")
    return out.toTypedArray()
}
