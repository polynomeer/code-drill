package parking;

/** 차와 자리의 크기. 순서가 곧 크기다 — SMALL < MEDIUM < LARGE. */
public enum VehicleSize {
    SMALL(1000), MEDIUM(2000), LARGE(3000);

    /** 이 크기의 자리에 댄 차의 시간당 요금. */
    public final long hourlyFee;

    VehicleSize(long hourlyFee) { this.hourlyFee = hourlyFee; }
}
