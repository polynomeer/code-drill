package parking;

import java.util.List;

/**
 * 주차장 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 */
public class ParkingLot {

    public ParkingLot(int small, int medium, int large) {
        throw new UnsupportedOperationException("TODO");
    }

    public Ticket park(String plate, VehicleSize size, long enteredAt) {
        throw new UnsupportedOperationException("TODO");
    }

    public long leave(String ticketId, long leftAt) {
        throw new UnsupportedOperationException("TODO");
    }

    public int free(VehicleSize size) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<String> parked() {
        throw new UnsupportedOperationException("TODO");
    }
}
