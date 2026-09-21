package tests;

import java.util.List;
import parking.ParkingExceptions.LotFullException;
import parking.ParkingLot;
import parking.Ticket;
import parking.VehicleSize;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicParkingTest {

    public void testParkAndLeaveChargesByStartedHour() {
        ParkingLot lot = new ParkingLot(1, 1, 1);
        Ticket ticket = lot.park("11가1111", VehicleSize.SMALL, 0);
        assertEquals(VehicleSize.SMALL, ticket.spot());
        assertEquals(List.of("11가1111"), lot.parked());
        assertEquals(2000L, lot.leave(ticket.id(), 90));
        assertEquals(List.of(), lot.parked());
    }

    public void testSmallCarTakesBiggerSpotWhenSmallIsFull() {
        ParkingLot lot = new ParkingLot(1, 1, 0);
        lot.park("A", VehicleSize.SMALL, 0);
        Ticket second = lot.park("B", VehicleSize.SMALL, 0);
        assertEquals(VehicleSize.MEDIUM, second.spot());
        assertEquals(0, lot.free(VehicleSize.MEDIUM));
    }

    public void testLotFull() {
        ParkingLot lot = new ParkingLot(0, 0, 1);
        lot.park("A", VehicleSize.LARGE, 0);
        assertThrows(LotFullException.class, () -> lot.park("B", VehicleSize.SMALL, 0));
    }
}
