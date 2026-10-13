// kind: WRONG_BRANCH
// 시각을 문자열로 정렬한다. 10:00 이 2:00 보다 앞에 온다 — 시각 순서여야 한다.
fun binaryWatch(lit: Int): Array<String> {
    val out = ArrayList<String>()
    for (hour in 0 until 12) for (minute in 0 until 60) if (Integer.bitCount(hour) + Integer.bitCount(minute) == lit) out.add("$hour:" + (if (minute < 10) "0" else "") + minute)
    return out.sorted().toTypedArray()
}
