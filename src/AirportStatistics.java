import java.util.ArrayList;
import java.util.List;

class AirportStatistics {

    private final List<Long> waitingTimesMillis = new ArrayList<>();

    private int planesServed = 0;
    private int passengersBoarded = 0;

    synchronized void recordWaitingTime(long waitingTimeMillis) {
        waitingTimesMillis.add(waitingTimeMillis);
    }

    synchronized void planeServed() {
        planesServed++;
    }

    synchronized void addPassengersBoarded(int passengers) {
        passengersBoarded += passengers;
    }

    synchronized Snapshot snapshot() {

        long minimumWait = waitingTimesMillis.stream()
                .min(Long::compareTo)
                .orElse(0L);

        long maximumWait = waitingTimesMillis.stream()
                .max(Long::compareTo)
                .orElse(0L);

        double averageWait = waitingTimesMillis.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        return new Snapshot(
                planesServed,
                passengersBoarded,
                minimumWait,
                averageWait,
                maximumWait
        );
    }

    static class Snapshot {

        final int planesServed;
        final int passengersBoarded;
        final long minimumWaitMillis;
        final double averageWaitMillis;
        final long maximumWaitMillis;

        Snapshot(
                int planesServed,
                int passengersBoarded,
                long minimumWaitMillis,
                double averageWaitMillis,
                long maximumWaitMillis
        ) {
            this.planesServed = planesServed;
            this.passengersBoarded = passengersBoarded;
            this.minimumWaitMillis = minimumWaitMillis;
            this.averageWaitMillis = averageWaitMillis;
            this.maximumWaitMillis = maximumWaitMillis;
        }
    }
}