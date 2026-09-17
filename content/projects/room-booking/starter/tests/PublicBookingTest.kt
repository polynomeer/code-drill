package tests

import booking.*
import codedrill.*

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicBookingTest {

    fun testBookAndListSortedByStart() {
        val room = RoomBooking(0, 600)
        room.book("b", 120, 180)
        room.book("a", 0, 60)
        assertEquals(listOf(Booking("a", 0, 60), Booking("b", 120, 180)), room.bookings())
    }

    fun testOverlapIsConflict() {
        val room = RoomBooking(0, 600)
        room.book("a", 60, 120)
        val error = assertThrows<ConflictException> { room.book("b", 90, 150) }
        assertEquals("a", error.withId)
        assertEquals(1, room.bookings().size)
    }

    fun testCancelAndFreeGapBetween() {
        val room = RoomBooking(0, 300)
        room.book("a", 0, 100)
        room.book("b", 200, 300)
        assertEquals(listOf(Slot(100, 200)), room.free())
        assertTrue(room.cancel("a"))
        assertFalse(room.cancel("a"))
        assertEquals(listOf(Slot(0, 200)), room.free())
    }
}
