package parking;

/** 주차표. 값 객체라 바뀌지 않는다. spot 은 차의 크기가 아니라 댄 자리의 크기다. */
public record Ticket(String id, String plate, VehicleSize spot, long enteredAt) {
}
