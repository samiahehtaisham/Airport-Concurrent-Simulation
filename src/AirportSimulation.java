import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

class AirportSimulation {

    static final int PLANE_COUNT = 8;
    static final int MAX_PLANES_ON_GROUND = 3;
    static final int MAX_PASSENGERS = 50;

    static final int LANDING_TIME_MS = 180;
    static final int TAXI_TIME_MS = 180;
    static final int DISEMBARK_TIME_MS = 240;
    static final int EMBARK_TIME_MS = 260;
    static final int SUPPLY_TIME_MS = 420;
    static final int CLEANING_TIME_MS = 380;
    static final int REFUEL_TIME_MS = 520;
    static final int TAKEOFF_TIME_MS = 180;

    final AirportStatistics statistics = new AirportStatistics();

    final CountDownLatch emergencyDocked =
            new CountDownLatch(1);

    final CountDownLatch firstTwoPlanesDocked =
            new CountDownLatch(2);

    final CountDownLatch firstTwoHoldingRequestsReceived =
            new CountDownLatch(2);

    final ExecutorService passengerWorkers =
            Executors.newFixedThreadPool(
                    3,
                    namedThreadFactory("Passenger")
            );

    final ExecutorService groundWorkers =
            Executors.newFixedThreadPool(
                    6,
                    namedThreadFactory("GroundCrew")
            );

    final ExecutorService fuelTruck =
            Executors.newSingleThreadExecutor(
                    namedThreadFactory("FuelTruck")
            );

    final AirTrafficController atc =
            new AirTrafficController(this);

    private final java.util.List<Thread> pilots =
            new java.util.ArrayList<>();

    public static void main(String[] args) {
        new AirportSimulation().run();
    }

    void run() {

        atc.start();

        Thread arrivalScheduler =
                new Thread(
                        this::scheduleFlights,
                        "Arrival-Scheduler"
                );

        arrivalScheduler.start();

        try {
            arrivalScheduler.join();

            for (Thread pilot : pilots) {
                pilot.join();
            }

            atc.finishAndReport();

            atc.join();

            passengerWorkers.shutdown();
            groundWorkers.shutdown();
            fuelTruck.shutdown();

            passengerWorkers.awaitTermination(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS
            );

            groundWorkers.awaitTermination(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS
            );

            fuelTruck.awaitTermination(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            passengerWorkers.shutdownNow();
            groundWorkers.shutdownNow();
            fuelTruck.shutdownNow();

            throw new IllegalStateException(
                    "Airport simulation interrupted",
                    e
            );
        }
    }

    private void scheduleFlights() {

        for (int number = 1; number <= PLANE_COUNT; number++) {

            int delaySeconds =
                    ThreadLocalRandom.current()
                            .nextInt(0, 3);

            pause(delaySeconds * 1000L);

            /*
             * Planes 1 and 2 must dock first.
             * This helps create the required congestion scenario.
             */
            if (number == 3) {

                try {
                    firstTwoPlanesDocked.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            /*
             * Planes 3 and 4 must have submitted their
             * landing requests before the emergency plane arrives.
             */
            if (number == 5) {

                try {
                    firstTwoHoldingRequestsReceived.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            boolean emergency = number == 5;

            int passengersArriving =
                    ThreadLocalRandom.current()
                            .nextInt(MAX_PASSENGERS + 1);

            int passengersBoarding =
                    ThreadLocalRandom.current()
                            .nextInt(MAX_PASSENGERS + 1);

            Flight flight =
                    new Flight(
                            this,
                            number,
                            emergency,
                            passengersArriving,
                            passengersBoarding
                    );

            Thread pilot =
                    new Thread(
                            flight,
                            "Pilot-Plane-" + number
                    );

            pilots.add(pilot);

            log(
                    "New "
                    + (emergency ? "EMERGENCY " : "")
                    + "Plane "
                    + number
                    + " has arrived"
            );

            pilot.start();
        }
    }

    static ThreadFactory namedThreadFactory(
            String prefix
    ) {

        AtomicInteger counter =
                new AtomicInteger(1);

        return runnable ->
                new Thread(
                        runnable,
                        prefix + "-" + counter.getAndIncrement()
                );
    }

    static void log(String message) {

        synchronized (System.out) {

            System.out.printf(
                    "%-20s | %s%n",
                    Thread.currentThread().getName(),
                    message
            );
        }
    }

    static void pause(long milliseconds) {

        try {
            Thread.sleep(milliseconds);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Thread interrupted",
                    e
            );
        }
    }
    static <T> T get(java.util.concurrent.Future<T> future) {

    try {
        return future.get();

    } catch (InterruptedException e) {

        Thread.currentThread().interrupt();

        throw new IllegalStateException(
                "Task interrupted",
                e
        );

    } catch (java.util.concurrent.ExecutionException e) {

        throw new IllegalStateException(
                "Task failed",
                e
        );
    }
}
}   