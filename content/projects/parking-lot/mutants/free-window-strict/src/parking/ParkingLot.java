package parking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import parking.ParkingExceptions.AlreadyParkedException;
import parking.ParkingExceptions.LotFullException;
import parking.ParkingExceptions.UnknownTicketException;

// kind: OFF_BY_ONE
// 30분 미만만 무료다. 정확히 30분은 무료여야 한다.
public class ParkingLot {

    private static final long FREE_MINUTES = 30;

    private final EnumMap<VehicleSize, Integer> free = new EnumMap<>(VehicleSize.class);
    private final Map<String, Ticket> active = new HashMap<>();   // ticket id → 표
    private final Map<String, String> byPlate = new HashMap<>();  // 번호판 → ticket id
    private long nextId = 1;

    public ParkingLot(int small, int medium, int large) {
        if (small < 0 || medium < 0 || large < 0) throw new IllegalArgumentException("spot counts must not be negative");
        free.put(VehicleSize.SMALL, small);
        free.put(VehicleSize.MEDIUM, medium);
        free.put(VehicleSize.LARGE, large);
    }

    public Ticket park(String plate, VehicleSize size, long enteredAt) {
        if (byPlate.containsKey(plate)) throw new AlreadyParkedException(plate);
        // 자기 크기부터 위로 — 맞는 자리 중 가장 작은 것.
        for (VehicleSize spot : VehicleSize.values()) {
            if (spot.compareTo(size) < 0 || free.get(spot) == 0) continue;
            free.put(spot, free.get(spot) - 1);
            Ticket ticket = new Ticket("T" + nextId++, plate, spot, enteredAt);
            active.put(ticket.id(), ticket);
            byPlate.put(plate, ticket.id());
            return ticket;
        }
        throw new LotFullException(size);
    }

    public long leave(String ticketId, long leftAt) {
        Ticket ticket = active.get(ticketId);
        if (ticket == null) throw new UnknownTicketException(ticketId);
        if (leftAt < ticket.enteredAt()) throw new IllegalArgumentException("left before entering: " + leftAt + " < " + ticket.enteredAt());
        // 검사가 다 지난 뒤에만 바꾼다.
        active.remove(ticketId);
        byPlate.remove(ticket.plate());
        free.put(ticket.spot(), free.get(ticket.spot()) + 1);
        return fee(ticket.spot(), leftAt - ticket.enteredAt());
    }

    /** 30분 이하 무료, 넘으면 시작한 시간 단위. */
    private static long fee(VehicleSize spot, long minutes) {
        if (minutes < FREE_MINUTES) return 0;
        long hours = (minutes + 59) / 60;
        return hours * spot.hourlyFee;
    }

    public int free(VehicleSize size) {
        return free.get(size);
    }

    public List<String> parked() {
        List<String> plates = new ArrayList<>(byPlate.keySet());
        Collections.sort(plates);
        return plates;
    }
}
