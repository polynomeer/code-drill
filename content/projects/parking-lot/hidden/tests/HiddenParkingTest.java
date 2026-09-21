package tests;

import java.util.List;
import parking.ParkingExceptions.AlreadyParkedException;
import parking.ParkingExceptions.LotFullException;
import parking.ParkingExceptions.UnknownTicketException;
import parking.ParkingLot;
import parking.Ticket;
import parking.VehicleSize;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenParkingTest {

    public void testThirtyMinutesIsFreeAndThirtyOneIsAnHour() {
        ParkingLot lot = new ParkingLot(2, 0, 0);
        Ticket a = lot.park("A", VehicleSize.SMALL, 100);
        Ticket b = lot.park("B", VehicleSize.SMALL, 100);
        assertEquals(0L, lot.leave(a.id(), 130));
        assertEquals(1000L, lot.leave(b.id(), 131));
    }

    public void testStartedHoursRoundUp() {
        ParkingLot lot = new ParkingLot(0, 3, 0);
        Ticket a = lot.park("A", VehicleSize.MEDIUM, 0);
        Ticket b = lot.park("B", VehicleSize.MEDIUM, 0);
        Ticket c = lot.park("C", VehicleSize.MEDIUM, 0);
        assertEquals(2000L, lot.leave(a.id(), 60));
        assertEquals(4000L, lot.leave(b.id(), 61));
        assertEquals(6000L, lot.leave(c.id(), 121));
    }

    public void testFeeFollowsSpotNotVehicle() {
        ParkingLot lot = new ParkingLot(0, 0, 1);
        Ticket t = lot.park("A", VehicleSize.SMALL, 0);
        assertEquals(VehicleSize.LARGE, t.spot());
        assertEquals(3000L, lot.leave(t.id(), 45));
    }

    public void testSmallestFittingSpotEvenWhenLargerIsFree() {
        ParkingLot lot = new ParkingLot(1, 1, 1);
        assertEquals(VehicleSize.MEDIUM, lot.park("M", VehicleSize.MEDIUM, 0).spot());
        assertEquals(VehicleSize.SMALL, lot.park("S", VehicleSize.SMALL, 0).spot());
        assertEquals(1, lot.free(VehicleSize.LARGE));
        assertEquals(VehicleSize.LARGE, lot.park("S2", VehicleSize.SMALL, 0).spot());
    }

    public void testLargeCannotUseSmallerSpot() {
        ParkingLot lot = new ParkingLot(5, 5, 0);
        assertThrows(LotFullException.class, () -> lot.park("L", VehicleSize.LARGE, 0));
        assertEquals(5, lot.free(VehicleSize.SMALL));
    }

    public void testLeaveFreesTheSpot() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park("A", VehicleSize.SMALL, 0);
        assertEquals(0, lot.free(VehicleSize.SMALL));
        lot.leave(t.id(), 10);
        assertEquals(1, lot.free(VehicleSize.SMALL));
        assertEquals(VehicleSize.SMALL, lot.park("B", VehicleSize.SMALL, 20).spot());
    }

    public void testTicketIsSingleUseAndUnknownIsRejected() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park("A", VehicleSize.SMALL, 0);
        lot.leave(t.id(), 10);
        assertThrows(UnknownTicketException.class, () -> lot.leave(t.id(), 20));
        assertThrows(UnknownTicketException.class, () -> lot.leave("nope", 20));
    }

    public void testLeavingBeforeEnteringKeepsTheCar() {
        ParkingLot lot = new ParkingLot(1, 0, 0);
        Ticket t = lot.park("A", VehicleSize.SMALL, 100);
        assertThrows(IllegalArgumentException.class, () -> lot.leave(t.id(), 99));
        assertEquals(List.of("A"), lot.parked());
        assertEquals(0, lot.free(VehicleSize.SMALL));
    }

    public void testSamePlateTwiceIsRejectedBeforeCapacity() {
        ParkingLot lot = new ParkingLot(0, 0, 1);
        lot.park("A", VehicleSize.SMALL, 0);
        assertThrows(AlreadyParkedException.class, () -> lot.park("A", VehicleSize.SMALL, 5));
        ParkingLot full = new ParkingLot(1, 0, 0);
        full.park("A", VehicleSize.SMALL, 0);
        assertThrows(AlreadyParkedException.class, () -> full.park("A", VehicleSize.SMALL, 5));
    }

    public void testParkedIsSortedAndIdsAreUnique() {
        ParkingLot lot = new ParkingLot(3, 0, 0);
        Ticket c = lot.park("C", VehicleSize.SMALL, 0);
        Ticket a = lot.park("A", VehicleSize.SMALL, 0);
        Ticket b = lot.park("B", VehicleSize.SMALL, 0);
        assertEquals(List.of("A", "B", "C"), lot.parked());
        assertFalse(a.id().equals(b.id()) || b.id().equals(c.id()) || a.id().equals(c.id()));
        assertThrows(IllegalArgumentException.class, () -> new ParkingLot(-1, 0, 0));
    }
}
