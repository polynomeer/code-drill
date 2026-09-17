package tests

import booking.*
import codedrill.*

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenBookingTest {

    fun testTouchingIntervalsDoNotConflict() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        room.book("b", 120, 180)
        room.book("c", 0, 60)
        assertEquals(listOf("c", "a", "b"), room.bookings().map { it.id })
    }

    fun testMoveOverlappingItsOwnSlot() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        assertEquals(Booking("a", 90, 150), room.move("a", 90, 150))
        assertEquals(listOf(Booking("a", 90, 150)), room.bookings())
    }

    fun testMoveIntoOtherBookingKeepsOriginal() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        room.book("b", 200, 260)
        val error = assertThrows<ConflictException> { room.move("a", 180, 240) }
        assertEquals("b", error.withId)
        assertEquals(listOf(Booking("a", 60, 120), Booking("b", 200, 260)), room.bookings())
    }

    fun testMoveWithBadArgumentsKeepsOriginal() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        assertThrows<IllegalArgumentException> { room.move("a", 500, 700) }
        assertThrows<IllegalArgumentException> { room.move("a", 120, 120) }
        assertEquals(listOf(Booking("a", 60, 120)), room.bookings())
        assertThrows<NoSuchBookingException> { room.move("zz", 0, 10) }
    }

    fun testFreeIncludesLeadingAndTrailingGaps() {
        val room = RoomBooking(0, 300)
        room.book("a", 100, 200)
        assertEquals(listOf(Slot(0, 100), Slot(200, 300)), room.free())
    }

    fun testFreeWithoutBookingsIsWholeDay() {
        val room = RoomBooking(30, 300)
        assertEquals(listOf(Slot(30, 300)), room.free())
    }

    fun testFreeSkipsEmptyGaps() {
        val room = RoomBooking(0, 300)
        room.book("a", 0, 100)
        room.book("b", 100, 200)
        room.book("c", 250, 300)
        assertEquals(listOf(Slot(200, 250)), room.free())
    }

    fun testArgumentChecksAndOrder() {
        assertThrows<IllegalArgumentException> { RoomBooking(100, 100) }
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        assertThrows<IllegalArgumentException> { room.book("b", 120, 100) }
        assertThrows<IllegalArgumentException> { room.book("b", -10, 30) }
        assertThrows<IllegalArgumentException> { room.book("b", 590, 601) }
        // 같은 id 는 겹침보다 먼저 본다.
        assertThrows<DuplicateBookingException> { room.book("a", 60, 120) }
        assertEquals(1, room.bookings().size)
    }

    fun testCancelFreesTheSlotForOthers() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        assertTrue(room.cancel("a"))
        room.book("b", 60, 120)
        assertEquals(listOf(Booking("b", 60, 120)), room.bookings())
    }
}
