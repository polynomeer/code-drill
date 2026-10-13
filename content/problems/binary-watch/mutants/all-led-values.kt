// kind: OFF_BY_ONE
// LED 가 나타낼 수 있는 값을 모두 쓴다 — 시 0..15, 분 0..63. 시는 11, 분은 59 까지다.
fun binaryWatch(lit: Int): Array<String> {
    val out = ArrayList<String>()
    for (hour in 0 until 16) for (minute in 0 until 64) if (Integer.bitCount(hour) + Integer.bitCount(minute) == lit) out.add("$hour:" + (if (minute < 10) "0" else "") + minute)
    return out.toTypedArray()
}
