package booking

// kind: MISSING_EDGE_CASE
// 옮길 때 자기 자신을 겹침에서 빼지 않는다 — 자기 자리와 겹치는 이동이 전부 막힌다.

class RoomBooking(val openAt: Int, val closeAt: Int) {
    private val bookings = HashMap<String, Booking>()

    init {
        require(openAt < closeAt) { "room must open before it closes: $openAt >= $closeAt" }
    }

    private fun validate(start: Int, end: Int) {
        require(start < end) { "start must be before end: $start >= $end" }
        require(start >= openAt && end <= closeAt) { "outside opening hours: [$start, $end)" }
    }

    /** 반열린 구간의 겹침 — 끝과 시작이 같으면 겹치지 않는다. [except] 는 옮기는 자기 자신이다. */
    private fun conflict(start: Int, end: Int, except: String?): Booking? =
        bookings.values.firstOrNull { it.id != except && it.start < end && start < it.end }

    fun book(id: String, start: Int, end: Int): Booking {
        validate(start, end)
        if (id in bookings) throw DuplicateBookingException(id)
        conflict(start, end, null)?.let { throw ConflictException(it.id) }
        return Booking(id, start, end).also { bookings[id] = it }
    }

    fun move(id: String, start: Int, end: Int): Booking {
        if (id !in bookings) throw NoSuchBookingException(id)
        validate(start, end)
        conflict(start, end, null)?.let { throw ConflictException(it.id) }
        // 검사가 전부 지난 뒤에만 바꾼다 — 실패하면 원래 예약이 남는다.
        return Booking(id, start, end).also { bookings[id] = it }
    }

    fun cancel(id: String): Boolean = bookings.remove(id) != null

    fun bookings(): List<Booking> = bookings.values.sortedBy { it.start }

    fun free(): List<Slot> {
        val slots = ArrayList<Slot>()
        var cursor = openAt
        for (booking in bookings()) {
            if (booking.start > cursor) slots += Slot(cursor, booking.start)
            cursor = booking.end
        }
        if (cursor < closeAt) slots += Slot(cursor, closeAt)
        return slots
    }
}
